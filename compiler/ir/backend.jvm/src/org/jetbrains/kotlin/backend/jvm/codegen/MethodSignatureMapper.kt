/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.backend.common.lower.parentsWithSelf
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.replaceValueParametersIn
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.codegen.state.extractTypeMappingModeFromAnnotation
import org.jetbrains.kotlin.codegen.state.isMethodWithDeclarationSiteWildcardsFqName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.descriptors.IrBasedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
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

    fun mapFunctionName(function: IrFunction, skipSpecial: Boolean = false): String {
        if (function !is IrSimpleFunction) return function.name.asString()

        if (!skipSpecial) {
            if (function.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) {
                val platformName = function.getJvmNameFromAnnotation()
                if (platformName != null) return platformName
            }

            val nameForSpecialFunction = getJvmMethodNameIfSpecial(function)
            if (nameForSpecialFunction != null) return nameForSpecialFunction
        }

        val property = function.correspondingPropertySymbol?.owner
        if (property != null) {
            val propertyName = property.name.asString()
            val propertyParent = property.parentAsClass
            if (propertyParent.isAnnotationClass || propertyParent.superTypes.any { it.isJavaLangRecord() }) return propertyName

            // The enum property getters <get-name> and <get-ordinal> have special names which also
            // apply to their fake overrides. Unfortunately, getJvmMethodNameIfSpecial does not handle
            // fake overrides, so we need a special case here.
            if ((propertyParent.isEnumClass || propertyParent.isEnumEntry) && (propertyName == "name" || propertyName == "ordinal"))
                return propertyName

            if (function.name.isSpecial) {
                val accessorName = if (function.isGetter) JvmAbi.getterName(propertyName) else JvmAbi.setterName(propertyName)
                return mangleMemberNameIfRequired(accessorName, function)
            }
        }

        return mangleMemberNameIfRequired(function.name.asString(), function)
    }

    private fun IrType.isJavaLangRecord() = getClass()!!.hasEqualFqName(JAVA_LANG_RECORD_FQ_NAME)

    private fun mangleMemberNameIfRequired(name: String, function: IrSimpleFunction): String {
        val newName = JvmCodegenUtil.sanitizeNameIfNeeded(name, context.state.languageVersionSettings)

        val suffix = if (function.isTopLevel) {
            if (function.isInvisibleInMultifilePart()) function.parentAsClass.name.asString() else null
        } else {
            function.getInternalFunctionForManglingIfNeeded()?.let {
                NameUtils.sanitizeAsJavaIdentifier(getModuleName(it))
            }
        } ?: return newName

        if (function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            assert(newName.endsWith(JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX)) { "Default adapter should end with \$default: ${function.render()}" }
            return newName.substringBeforeLast(JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX) + "$" + suffix + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX
        }

        return "$newName$$suffix"
    }

    private fun IrSimpleFunction.isInvisibleInMultifilePart(): Boolean =
        name.asString() != "<clinit>" &&
                (parent as? IrClass)?.attributeOwnerId in context.multifileFacadeForPart.keys &&
                (DescriptorVisibilities.isPrivate(suspendFunctionOriginal().visibility) ||
                        originalForDefaultAdapter?.isInvisibleInMultifilePart() == true)

    private fun IrSimpleFunction.getInternalFunctionForManglingIfNeeded(): IrSimpleFunction? {
        if (visibility == DescriptorVisibilities.INTERNAL &&
            origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR &&
            origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS &&
            origin != IrDeclarationOrigin.PROPERTY_DELEGATE &&
            !isPublishedApi()
        ) {
            return originalFunction.takeIf { it != this }
                ?.safeAs<IrSimpleFunction>()
                ?.getInternalFunctionForManglingIfNeeded()
                ?: this
        }
        originalForDefaultAdapter?.getInternalFunctionForManglingIfNeeded()?.let { return it }
        return null
    }

    private val IrSimpleFunction.originalForDefaultAdapter: IrSimpleFunction?
        get() = if (origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            (attributeOwnerId as IrFunction).symbol.owner as IrSimpleFunction
        } else null

    private fun getModuleName(function: IrSimpleFunction): String =
        (if (function is IrLazyFunctionBase)
            getJvmModuleNameForDeserialized(function)
        else null) ?: context.state.moduleName

    private fun IrSimpleFunction.isPublishedApi(): Boolean =
        propertyIfAccessor.annotations.hasAnnotation(StandardNames.FqNames.publishedApi)

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

        val mappingMode = typeSystem.getOptimalModeForReturnType(returnType, isAnnotationMethod)

        return typeMapper.mapType(returnType, mappingMode, sw)
    }

    private fun hasVoidReturnType(function: IrFunction): Boolean =
        function is IrConstructor || (function.returnType.isUnit() && !function.isGetter)

    // See also: KotlinTypeMapper.forceBoxedReturnType
    private fun forceBoxedReturnType(function: IrFunction): Boolean =
        isBoxMethodForInlineClass(function) ||
                forceFoxedReturnTypeOnOverride(function) ||
                forceBoxedReturnTypeOnDefaultImplFun(function) ||
                function.isFromJava() && function.returnType.isInlineClassType()

    private fun forceFoxedReturnTypeOnOverride(function: IrFunction) =
        function is IrSimpleFunction &&
                function.returnType.isPrimitiveType() &&
                function.allOverridden().any { !it.returnType.isPrimitiveType() }

    private fun forceBoxedReturnTypeOnDefaultImplFun(function: IrFunction): Boolean {
        if (function !is IrSimpleFunction) return false
        val originalFun = context.cachedDeclarations.getOriginalFunctionForDefaultImpl(function) ?: return false
        return forceFoxedReturnTypeOnOverride(originalFun)
    }

    private fun isBoxMethodForInlineClass(function: IrFunction): Boolean =
        function.parent.let { it is IrClass && it.isInline } &&
                function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER &&
                function.name.asString() == "box-impl"

    fun mapSignatureSkipGeneric(function: IrFunction): JvmMethodSignature =
        mapSignature(function, true)

    fun mapSignatureWithGeneric(function: IrFunction): JvmMethodGenericSignature =
        mapSignature(function, false)

    private fun mapSignature(function: IrFunction, skipGenericSignature: Boolean, skipSpecial: Boolean = false): JvmMethodGenericSignature {
        if (function is IrLazyFunctionBase && !function.isFakeOverride && function.initialSignatureFunction != null) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if ((function as? IrSimpleFunction)?.getDifferentNameForJvmBuiltinFunction() == null ||
                (function.parent as? IrClass)?.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            ) {
                return mapSignature(function.initialSignatureFunction!!, skipGenericSignature)
            }
        }

        val sw = if (skipGenericSignature) JvmSignatureWriter() else BothSignatureWriter(BothSignatureWriter.Mode.METHOD)

        typeMapper.writeFormalTypeParameters(function.typeParameters, sw)

        sw.writeParametersStart()

        val contextReceivers = function.valueParameters.subList(0, function.contextReceiverParametersCount)
        for (contextReceiver in contextReceivers) {
            writeParameter(sw, JvmMethodParameterKind.CONTEXT_RECEIVER, contextReceiver.type, function)
        }

        val receiverParameter = function.extensionReceiverParameter
        if (receiverParameter != null) {
            writeParameter(sw, JvmMethodParameterKind.RECEIVER, receiverParameter.type, function)
        }

        val regularValueParameters =
            function.valueParameters.subList(function.contextReceiverParametersCount, function.valueParameters.size)
        for (parameter in regularValueParameters) {
            val kind = when (parameter.origin) {
                JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS -> JvmMethodParameterKind.OUTER
                JvmLoweredDeclarationOrigin.ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER -> JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL
                else -> JvmMethodParameterKind.VALUE
            }
            val type =
                if (shouldBoxSingleValueParameterForSpecialCaseOfRemove(function))
                    parameter.type.makeNullable()
                else parameter.type
            writeParameter(sw, kind, type, function)
        }

        sw.writeReturnType()
        mapReturnType(function, sw)
        sw.writeReturnTypeEnd()

        val signature = sw.makeJvmMethodSignature(mapFunctionName(function, skipSpecial))

        val specialSignatureInfo =
            with(BuiltinMethodsWithSpecialGenericSignature) {
                function.toIrBasedDescriptorWithOriginalOverrides().getSpecialSignatureInfo()
            }

        // Old back-end doesn't patch generic signatures if corresponding function had special bridges.
        // See org.jetbrains.kotlin.codegen.FunctionCodegen#hasSpecialBridgeMethod and its usage.
        if (specialSignatureInfo != null && function !in context.functionsWithSpecialBridges) {
            val newGenericSignature = specialSignatureInfo.replaceValueParametersIn(signature.genericsSignature)
            return JvmMethodGenericSignature(signature.asmMethod, signature.valueParameters, newGenericSignature)
        }
        if (function.origin == JvmLoweredDeclarationOrigin.ABSTRACT_BRIDGE_STUB) {
            return JvmMethodGenericSignature(signature.asmMethod, signature.valueParameters, null)
        }

        return signature
    }

    private fun IrFunction.toIrBasedDescriptorWithOriginalOverrides(): FunctionDescriptor =
        when (this) {
            is IrConstructor ->
                toIrBasedDescriptor()
            is IrSimpleFunction ->
                if (isPropertyAccessor)
                    toIrBasedDescriptor()
                else
                    IrBasedSimpleFunctionDescriptorWithOriginalOverrides(this, context)
            else ->
                throw AssertionError("Unexpected function kind: $this")
        }

    private class IrBasedSimpleFunctionDescriptorWithOriginalOverrides(
        owner: IrSimpleFunction,
        private val context: JvmBackendContext
    ) : IrBasedSimpleFunctionDescriptor(owner) {
        override fun getOverriddenDescriptors(): List<FunctionDescriptor> =
            context.getOverridesWithoutStubs(owner).map {
                IrBasedSimpleFunctionDescriptorWithOriginalOverrides(it.owner, context)
            }
    }

    // Boxing is only necessary for 'remove(E): Boolean' of a MutableCollection<Int> implementation.
    // Otherwise this method might clash with 'remove(I): E' defined in the java.util.List JDK interface (mapped to kotlin 'removeAt').
    fun shouldBoxSingleValueParameterForSpecialCaseOfRemove(irFunction: IrFunction): Boolean {
        if (irFunction !is IrSimpleFunction) return false
        if (irFunction.name.asString() != "remove" && !irFunction.name.asString().startsWith("remove-")) return false
        if (irFunction.isFromJava()) return false
        if (irFunction.valueParameters.size != 1) return false
        val valueParameterType = irFunction.valueParameters[0].type
        if (!valueParameterType.unboxInlineClass().isInt()) return false
        return irFunction.allOverridden(false).any { it.parent.kotlinFqName == StandardNames.FqNames.mutableCollection }
    }

    private fun writeParameter(
        sw: JvmSignatureWriter,
        kind: JvmMethodParameterKind,
        type: IrType,
        function: IrFunction
    ) {
        sw.writeParameterType(kind)
        writeParameterType(sw, type, function)
        sw.writeParameterTypeEnd()
    }

    private fun writeParameterType(sw: JvmSignatureWriter, type: IrType, declaration: IrDeclaration) {
        if (sw.skipGenericSignature()) {
            if (type.isInlineClassType() && declaration.isFromJava()) {
                typeMapper.mapType(type, TypeMappingMode.GENERIC_ARGUMENT, sw)
            } else {
                typeMapper.mapType(type, TypeMappingMode.DEFAULT, sw)
            }
            return
        }

        val mode = with(typeSystem) {
            extractTypeMappingModeFromAnnotation(
                declaration.suppressWildcardsMode(), type, isForAnnotationParameter = false, mapTypeAliases = false
            )
                ?: if (declaration.isMethodWithDeclarationSiteWildcards && type.argumentsCount() != 0) {
                    TypeMappingMode.GENERIC_ARGUMENT // Render all wildcards
                } else {
                    typeSystem.getOptimalModeForValueParameter(type)
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

    // TODO get rid of 'caller' argument
    internal fun mapToCallableMethod(expression: IrCall, caller: IrFunction?): IrCallableMethod {
        val callee = expression.symbol.owner
        val calleeParent = expression.superQualifierSymbol?.owner
            ?: expression.dispatchReceiver?.type?.classOrNull?.owner?.let {
                // Calling Object class methods on interfaces is permitted, but they're not interface methods.
                if (it.isJvmInterface && callee.isMethodOfAny()) context.irBuiltIns.anyClass.owner else it
            }
            ?: callee.parentAsClass // Static call or type parameter
        val owner = typeMapper.mapOwner(calleeParent)

        val isInterface = calleeParent.isJvmInterface
        val isSuperCall = expression.superQualifierSymbol != null

        val invokeOpcode = when {
            callee.dispatchReceiverParameter == null -> Opcodes.INVOKESTATIC
            isSuperCall -> Opcodes.INVOKESPECIAL
            isInterface && !DescriptorVisibilities.isPrivate(callee.visibility) -> Opcodes.INVOKEINTERFACE
            DescriptorVisibilities.isPrivate(callee.visibility) -> Opcodes.INVOKESPECIAL
            else -> Opcodes.INVOKEVIRTUAL
        }

        val declaration = findSuperDeclaration(callee, isSuperCall)
        val signature =
            if (caller != null && caller.isBridge()) {
                // Do not remap special builtin methods when called from a bridge. The bridges are there to provide the
                // remapped name or signature and forward to the actually declared method.
                mapSignatureSkipGeneric(declaration)
            } else {
                mapOverriddenSpecialBuiltinIfNeeded(declaration, isSuperCall)
                    ?: mapSignatureSkipGeneric(declaration)
            }

        return IrCallableMethod(owner, invokeOpcode, signature, isInterface, declaration.returnType)
    }

    // TODO: get rid of this (probably via some special lowering)
    private fun mapOverriddenSpecialBuiltinIfNeeded(callee: IrFunction, superCall: Boolean): JvmMethodSignature? {
        // Do not remap calls to static replacements of inline class methods, since they have completely different signatures.
        if (callee.isStaticInlineClassReplacement) return null
        val overriddenSpecialBuiltinFunction =
            (callee.toIrBasedDescriptor().getOverriddenBuiltinReflectingJvmDescriptor() as IrBasedSimpleFunctionDescriptor?)?.owner
        if (overriddenSpecialBuiltinFunction != null && !superCall) {
            return mapSignatureSkipGeneric(overriddenSpecialBuiltinFunction)
        }

        return null
    }

    fun mapCalleeToAsmMethod(function: IrSimpleFunction, isSuperCall: Boolean = false): Method =
        mapAsmMethod(findSuperDeclaration(function, isSuperCall))

    private fun findSuperDeclaration(function: IrSimpleFunction, isSuperCall: Boolean): IrSimpleFunction =
        findSuperDeclaration(function, isSuperCall, context.state.jvmDefaultMode)

    private fun getJvmMethodNameIfSpecial(irFunction: IrSimpleFunction): String? {
        if (irFunction.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) {
            return null
        }

        return irFunction.getBuiltinSpecialPropertyGetterName()
            ?: irFunction.getDifferentNameForJvmBuiltinFunction()
    }

    private val IrSimpleFunction.isBuiltIn: Boolean
        get() = getPackageFragment()?.fqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME ||
                parent.safeAs<IrClass>()?.fqNameWhenAvailable?.toUnsafe()?.let(JavaToKotlinClassMap::mapKotlinToJava) != null

    // From BuiltinMethodsWithDifferentJvmName.isBuiltinFunctionWithDifferentNameInJvm, BuiltinMethodsWithDifferentJvmName.getJvmName
    private fun IrSimpleFunction.getDifferentNameForJvmBuiltinFunction(): String? {
        if (name !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES) return null
        if (!isBuiltIn) return null
        return allOverridden(includeSelf = true)
            .filter { it.isBuiltIn }
            .mapNotNull {
                val signature = it.computeJvmSignature()
                SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME[signature]?.asString()
            }
            .firstOrNull()
    }

    private fun IrSimpleFunction.getBuiltinSpecialPropertyGetterName(): String? {
        val propertyName = correspondingPropertySymbol?.owner?.name ?: return null
        if (propertyName !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null
        if (!isBuiltIn) return null
        return allOverridden(includeSelf = true)
            .mapNotNull {
                val property = it.correspondingPropertySymbol!!.owner
                BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[property.fqNameWhenAvailable]?.asString()
            }
            .firstOrNull()
    }

    private fun IrFunction.computeJvmSignature(): String = signatures {
        val classPart = typeMapper.mapType(parentAsClass.defaultType).internalName
        val signature = mapSignature(this@computeJvmSignature, skipGenericSignature = false, skipSpecial = true).toString()
        return signature(classPart, signature)
    }

    // From org.jetbrains.kotlin.load.kotlin.getJvmModuleNameForDeserializedDescriptor
    private fun getJvmModuleNameForDeserialized(function: IrLazyFunctionBase): String? {
        var current: IrDeclarationParent? = function.parent
        while (current != null) {
            when (current) {
                is IrLazyClass -> {
                    val classProto = current.classProto ?: return null
                    val nameResolver = current.nameResolver ?: return null
                    return classProto.getExtensionOrNull(JvmProtoBuf.classModuleName)
                        ?.let(nameResolver::getString)
                        ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
                }
                is IrMaybeDeserializedClass ->
                    return current.moduleName
                is IrExternalPackageFragment -> {
                    val source = current.containerSource ?: return null
                    return (source as? JvmPackagePartSource)?.moduleName
                }
                else -> current = (current as? IrDeclaration)?.parent ?: return null
            }
        }
        return null
    }
}
