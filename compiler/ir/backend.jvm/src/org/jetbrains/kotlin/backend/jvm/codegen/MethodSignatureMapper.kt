/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.backend.common.lower.allOverridden
import org.jetbrains.kotlin.backend.common.lower.parentsWithSelf
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.backend.jvm.ir.isCompiledToJvmDefault
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.replaceValueParametersIn
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.codegen.state.extractTypeMappingModeFromAnnotation
import org.jetbrains.kotlin.codegen.state.isMethodWithDeclarationSiteWildcardsFqName
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.getJvmMethodNameIfSpecial
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.forceSingleValueParameterBoxing
import org.jetbrains.kotlin.load.kotlin.getJvmModuleNameForDeserializedDescriptor
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class MethodSignatureMapper(private val context: JvmBackendContext) {
    private val typeMapper: IrTypeMapper = context.typeMapper
    private val typeSystem: IrTypeSystemContext = typeMapper.typeSystem

    fun mapAsmMethod(function: IrFunction): Method =
        mapSignatureSkipGeneric(function).asmMethod

    fun mapFieldSignature(field: IrField): String? {
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
        if (field.correspondingPropertySymbol?.owner?.isVar == true) {
            writeParameterType(sw, field.type, field)
        } else {
            mapReturnType(field, field.type, sw)
        }
        return sw.makeJavaGenericSignature()
    }

    fun mapFunctionName(function: IrFunction): String {
        if (function.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) {
            val platformName = function.getJvmNameFromAnnotation()
            if (platformName != null) return platformName
        }

        val nameForSpecialFunction = getJvmMethodNameIfSpecial(function.descriptor)
        if (nameForSpecialFunction != null) return nameForSpecialFunction

        val property = (function as? IrSimpleFunction)?.correspondingPropertySymbol?.owner
        if (property != null && function.name.isSpecial) {
            val propertyName = property.name.asString()
            val propertyParent = property.parentAsClass
            if (propertyParent.isAnnotationClass)
                return propertyName

            // The enum property getters <get-name> and <get-ordinal> have special names which also
            // apply to their fake overrides. Unfortunately, getJvmMethodNameIfSpecial does not handle
            // fake overrides, so we need a special case here.
            if ((propertyParent.isEnumClass || propertyParent.isEnumEntry) && (propertyName == "name" || propertyName == "ordinal"))
                return propertyName

            val accessorName = if (function.isGetter) JvmAbi.getterName(propertyName) else JvmAbi.setterName(propertyName)
            return mangleMemberNameIfRequired(accessorName, function)
        }

        return mangleMemberNameIfRequired(function.name.asString(), function)
    }

    private fun mangleMemberNameIfRequired(name: String, function: IrFunction): String {
        val newName = JvmCodegenUtil.sanitizeNameIfNeeded(name, context.state.languageVersionSettings)

        if (function.isTopLevel) {
            if (Visibilities.isPrivate(function.suspendFunctionOriginal().visibility) &&
                newName != "<clinit>" && (function.parent as? IrClass)?.attributeOwnerId in context.multifileFacadeForPart
            ) {
                return "$newName$${function.parentAsClass.name.asString()}"
            }
            return newName
        }

        return if (function !is IrConstructor && function.visibility == Visibilities.INTERNAL && !function.isPublishedApi()) {
            KotlinTypeMapper.InternalNameMapper.mangleInternalName(newName, getModuleName(function))
        } else newName
    }

    private fun getModuleName(function: IrFunction): String =
        // TODO: get rid of descriptors here
        (if (function is IrLazyFunctionBase)
            getJvmModuleNameForDeserializedDescriptor(function.descriptor)
        else null) ?: context.state.moduleName

    private fun IrFunction.isPublishedApi(): Boolean =
        propertyIfAccessor.annotations.hasAnnotation(KotlinBuiltIns.FQ_NAMES.publishedApi)

    fun mapReturnType(declaration: IrDeclaration, sw: JvmSignatureWriter? = null): Type {
        if (declaration !is IrFunction) {
            require(declaration is IrField) { "Unsupported declaration: $declaration" }
            return mapReturnType(declaration, declaration.type, sw)
        }

        return when {
            hasVoidReturnType(declaration) -> {
                sw?.writeAsmType(Type.VOID_TYPE)
                Type.VOID_TYPE
            }
            forceBoxedReturnType(declaration) -> {
                typeMapper.mapType(declaration.returnType, TypeMappingMode.RETURN_TYPE_BOXED, sw)
            }
            else -> mapReturnType(declaration, declaration.returnType, sw)
        }
    }

    private fun mapReturnType(declaration: IrDeclaration, returnType: IrType, sw: JvmSignatureWriter?): Type {
        val isAnnotationMethod = declaration.parent.let { it is IrClass && it.isAnnotationClass }
        if (sw == null || sw.skipGenericSignature()) {
            return typeMapper.mapType(returnType, TypeMappingMode.getModeForReturnTypeNoGeneric(isAnnotationMethod), sw)
        }

        val typeMappingModeFromAnnotation =
            typeSystem.extractTypeMappingModeFromAnnotation(
                declaration.suppressWildcardsMode(), returnType, isAnnotationMethod, mapTypeAliases = false
            )
        if (typeMappingModeFromAnnotation != null) {
            return typeMapper.mapType(returnType, typeMappingModeFromAnnotation, sw)
        }

        val mappingMode = TypeMappingMode.getOptimalModeForReturnType(returnType.toKotlinType(), isAnnotationMethod)

        return typeMapper.mapType(returnType, mappingMode, sw)
    }

    private fun hasVoidReturnType(function: IrFunction): Boolean =
        function is IrConstructor || (function.returnType.isUnit() && !function.isGetter)

    // Copied from KotlinTypeMapper.forceBoxedReturnType.
    private fun forceBoxedReturnType(function: IrFunction): Boolean {
        if (isBoxMethodForInlineClass(function)) return true

        return isJvmPrimitiveOrInlineClass(function.returnType) &&
                function is IrSimpleFunction && function.allOverridden().any { !isJvmPrimitiveOrInlineClass(it.returnType) }
    }

    private fun isBoxMethodForInlineClass(function: IrFunction): Boolean =
        function.parent.let { it is IrClass && it.isInline } &&
                function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER &&
                function.name.asString() == "box-impl"

    private fun isJvmPrimitiveOrInlineClass(type: IrType): Boolean =
        type.isPrimitiveType() || type.getClass()?.isInline == true

    fun mapSignatureSkipGeneric(function: IrFunction): JvmMethodSignature =
        mapSignature(function, true)

    fun mapSignatureWithGeneric(function: IrFunction): JvmMethodGenericSignature =
        mapSignature(function, false)

    private fun mapSignature(function: IrFunction, skipGenericSignature: Boolean): JvmMethodGenericSignature {
        if (function is IrLazyFunctionBase && function.initialSignatureFunction != null) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if (function.descriptor.getOverriddenBuiltinReflectingJvmDescriptor() == null ||
                function.descriptor.containingDeclaration.original is JavaClassDescriptor
            ) {
                return mapSignature(function.initialSignatureFunction!!, skipGenericSignature)
            }
        }

        val sw = if (skipGenericSignature) JvmSignatureWriter() else BothSignatureWriter(BothSignatureWriter.Mode.METHOD)

        typeMapper.writeFormalTypeParameters(function.typeParameters, sw)

        sw.writeParametersStart()

        val receiverParameter = function.extensionReceiverParameter
        if (receiverParameter != null) {
            writeParameter(sw, JvmMethodParameterKind.RECEIVER, receiverParameter.type, function)
        }

        for (parameter in function.valueParameters) {
            val kind = when (parameter.origin) {
                JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS -> JvmMethodParameterKind.OUTER
                JvmLoweredDeclarationOrigin.ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER -> JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL
                else -> JvmMethodParameterKind.VALUE
            }
            val type = if (forceSingleValueParameterBoxing(function.descriptor)) parameter.type.makeNullable() else parameter.type
            writeParameter(sw, kind, type, function)
        }

        sw.writeReturnType()
        mapReturnType(function, sw)
        sw.writeReturnTypeEnd()

        val signature = sw.makeJvmMethodSignature(mapFunctionName(function))

        val specialSignatureInfo = with(BuiltinMethodsWithSpecialGenericSignature) { function.descriptor.getSpecialSignatureInfo() }

        if (specialSignatureInfo != null) {
            val newGenericSignature = specialSignatureInfo.replaceValueParametersIn(signature.genericsSignature)
            return JvmMethodGenericSignature(signature.asmMethod, signature.valueParameters, newGenericSignature)
        }

        return signature
    }

    private fun writeParameter(sw: JvmSignatureWriter, kind: JvmMethodParameterKind, type: IrType, function: IrFunction) {
        sw.writeParameterType(kind)
        writeParameterType(sw, type, function)
        sw.writeParameterTypeEnd()
    }

    private fun writeParameterType(sw: JvmSignatureWriter, type: IrType, declaration: IrDeclaration) {
        if (sw.skipGenericSignature()) {
            typeMapper.mapType(type, TypeMappingMode.DEFAULT, sw)
            return
        }

        val mode = with(typeSystem) {
            extractTypeMappingModeFromAnnotation(
                declaration.suppressWildcardsMode(), type, isForAnnotationParameter = false, mapTypeAliases = false
            )
                ?: if (declaration.isMethodWithDeclarationSiteWildcards && type.argumentsCount() != 0) {
                    TypeMappingMode.GENERIC_ARGUMENT // Render all wildcards
                } else {
                    TypeMappingMode.getOptimalModeForValueParameter(type.toKotlinType())
                }
        }

        typeMapper.mapType(type, mode, sw)
    }

    private val IrDeclaration.isMethodWithDeclarationSiteWildcards: Boolean
        get() = this is IrSimpleFunction && allOverridden().any {
            it.fqNameWhenAvailable.isMethodWithDeclarationSiteWildcardsFqName
        }

    private fun IrDeclaration.suppressWildcardsMode(): Boolean? =
        parentsWithSelf.mapNotNull { declaration ->
            when (declaration) {
                is IrField -> {
                    // Annotations on properties (JvmSuppressWildcards has PROPERTY, not FIELD, in its targets) have been moved
                    // to the synthetic "$annotations" method, but the copy can still be found via the property symbol.
                    declaration.correspondingPropertySymbol?.owner?.getSuppressWildcardsAnnotationValue()
                }
                is IrAnnotationContainer -> declaration.getSuppressWildcardsAnnotationValue()
                else -> null
            }
        }.firstOrNull()

    private fun IrAnnotationContainer.getSuppressWildcardsAnnotationValue(): Boolean? =
        getAnnotation(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME)?.run {
            if (valueArgumentsCount > 0) (getValueArgument(0) as? IrConst<*>)?.value as? Boolean ?: true else null
        }

    private fun IrFunctionAccessExpression.computeCalleeParent(): IrClass {
        if (this is IrCall) {
            superQualifierSymbol?.let { return it.owner }
        }
        return dispatchReceiver?.type?.classOrNull?.owner
            ?: symbol.owner.parentAsClass // Static call or type parameter
    }

    fun mapToCallableMethod(caller: IrFunction, expression: IrFunctionAccessExpression): IrCallableMethod {
        val callee = expression.symbol.owner
        val calleeParent = expression.computeCalleeParent()
        val owner = typeMapper.mapOwner(calleeParent)

        if (callee !is IrSimpleFunction) {
            check(callee is IrConstructor) { "Function must be a simple function or a constructor: ${callee.render()}" }
            return IrCallableMethod(owner, Opcodes.INVOKESPECIAL, mapSignatureSkipGeneric(callee), false)
        }

        val isInterface = calleeParent.isJvmInterface
        val isSuperCall = (expression as? IrCall)?.superQualifierSymbol != null

        val invokeOpcode = when {
            callee.dispatchReceiverParameter == null -> Opcodes.INVOKESTATIC
            isSuperCall -> Opcodes.INVOKESPECIAL
            isInterface && !Visibilities.isPrivate(callee.visibility) -> Opcodes.INVOKEINTERFACE
            Visibilities.isPrivate(callee.visibility) && !callee.isSuspend -> Opcodes.INVOKESPECIAL
            else -> Opcodes.INVOKEVIRTUAL
        }

        val declaration = findSuperDeclaration(callee, isSuperCall)
        val signature = mapOverriddenSpecialBuiltinIfNeeded(caller, declaration, isSuperCall)
            ?: mapSignatureSkipGeneric(declaration)

        return IrCallableMethod(owner, invokeOpcode, signature, isInterface)
    }

    // TODO: get rid of this (probably via some special lowering)
    private fun mapOverriddenSpecialBuiltinIfNeeded(caller: IrFunction, callee: IrFunction, superCall: Boolean): JvmMethodSignature? {
        // Do not remap special builtin methods when called from a bridge. The bridges are there to provide the
        // remapped name or signature and forward to the actually declared method.
        if (caller.origin == IrDeclarationOrigin.BRIDGE || caller.origin == IrDeclarationOrigin.BRIDGE_SPECIAL) return null
        // Do not remap calls to static replacements of inline class methods, since they have completely different signatures.
        if (callee.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) return null
        val overriddenSpecialBuiltinFunction = callee.descriptor.original.getOverriddenBuiltinReflectingJvmDescriptor()
        if (overriddenSpecialBuiltinFunction != null && !superCall) {
            return mapSignatureSkipGeneric(context.referenceFunction(overriddenSpecialBuiltinFunction.original).owner)
        }

        return null
    }

    // Copied from KotlinTypeMapper.findSuperDeclaration.
    private fun findSuperDeclaration(function: IrSimpleFunction, isSuperCall: Boolean): IrSimpleFunction {
        var current = function
        while (current.isFakeOverride) {
            // TODO: probably isJvmInterface instead of isInterface, here and in KotlinTypeMapper
            val classCallable = current.overriddenSymbols.firstOrNull { !it.owner.parentAsClass.isInterface }?.owner
            if (classCallable != null) {
                current = classCallable
                continue
            }
            if (isSuperCall && !current.parentAsClass.isInterface &&
                current.resolveFakeOverride()?.run {
                    isMethodOfAny() || !isCompiledToJvmDefault(context.state.jvmDefaultMode)
                } == true
            ) {
                return current
            }

            current = current.overriddenSymbols.firstOrNull()?.owner
                ?: error("Fake override should have at least one overridden descriptor: ${current.render()}")
        }
        return current
    }
}
