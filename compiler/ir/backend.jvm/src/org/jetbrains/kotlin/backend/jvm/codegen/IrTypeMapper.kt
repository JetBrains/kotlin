/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.AbstractTypeMapper
import org.jetbrains.kotlin.types.TypeMappingContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContextForTypeMapping
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.kotlin.backend.jvm.codegen.isRawType as isRawTypeImpl
import org.jetbrains.kotlin.ir.types.isKClass as isKClassImpl
import org.jetbrains.kotlin.ir.util.isSuspendFunction as isSuspendFunctionImpl

class IrTypeMapper(private val context: JvmBackendContext) : KotlinTypeMapperBase(), TypeMappingContext<JvmSignatureWriter> {
    internal val typeSystem = IrTypeSystemContextImpl(context.irBuiltIns)
    override val typeContext: TypeSystemCommonBackendContextForTypeMapping = IrTypeCheckerContextForTypeMapping(typeSystem, context)

    override fun mapClass(classifier: ClassifierDescriptor): Type =
        when (classifier) {
            is ClassDescriptor ->
                mapClass(context.referenceClass(classifier).owner)
            is TypeParameterDescriptor ->
                mapType(context.referenceTypeParameter(classifier).defaultType)
            else ->
                error("Unknown descriptor: $classifier")
        }

    private fun computeClassInternalName(irClass: IrClass): StringBuilder {
        context.getLocalClassType(irClass)?.internalName?.let {
            return StringBuilder(it)
        }

        val shortName = SpecialNames.safeIdentifier(irClass.name).identifier

        when (val parent = irClass.parent) {
            is IrPackageFragment ->
                return StringBuilder().apply {
                    val fqName = parent.fqName
                    if (!fqName.isRoot) {
                        append(fqName.asString().replace('.', '/')).append("/")
                    }
                    append(shortName)
                }
            is IrClass ->
                return computeClassInternalName(parent).append("$").append(shortName)
            is IrFunction ->
                if (parent.isSuspend && parent.parentAsClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
                    return computeClassInternalName(parent.parentAsClass.parentAsClass)
                        .append("$").append(parent.name.asString())
                }
        }

        error(
            "Local class should have its name computed in InventNamesForLocalClasses: ${irClass.fqNameWhenAvailable}\n" +
                    "Ensure that any lowering that transforms elements with local class info (classes, function references) " +
                    "invokes `copyAttributes` on the transformed element."
        )
    }

    fun classInternalName(irClass: IrClass): String {
        context.getLocalClassType(irClass)?.internalName?.let { return it }
        context.classNameOverride[irClass]?.let { return it.internalName }

        return JvmCodegenUtil.sanitizeNameIfNeeded(
            computeClassInternalName(irClass).toString(),
            context.state.languageVersionSettings
        )
    }

    override fun getClassInternalName(typeConstructor: TypeConstructorMarker): String =
        classInternalName((typeConstructor as IrClassSymbol).owner)

    fun writeFormalTypeParameters(irParameters: List<IrTypeParameter>, sw: JvmSignatureWriter) {
        if (sw.skipGenericSignature()) return
        with(KotlinTypeMapper) {
            for (typeParameter in irParameters) {
                typeSystem.writeFormalTypeParameter(typeParameter.symbol, sw) { type, mode ->
                    mapType(type as IrType, mode, sw)
                }
            }
        }
    }

    fun boxType(irType: IrType): Type {
        val irClass = irType.classOrNull?.owner
        if (irClass != null && irClass.isInline) {
            return mapTypeAsDeclaration(irType)
        }
        val type = AbstractTypeMapper.mapType(this, irType)
        return AsmUtil.boxPrimitiveType(type) ?: type
    }

    fun mapType(
        type: IrType,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: JvmSignatureWriter? = null
    ): Type = AbstractTypeMapper.mapType(this, type, mode, sw)

    override fun JvmSignatureWriter.writeGenericType(type: SimpleTypeMarker, asmType: Type, mode: TypeMappingMode) {
        require(type is IrSimpleType)
        if (skipGenericSignature() || hasNothingInNonContravariantPosition(type) || type.arguments.isEmpty() || type.isRawTypeImpl()) {
            writeAsmType(asmType)
            return
        }

        val possiblyInnerType = type.buildPossiblyInnerType() ?: error("possiblyInnerType with arguments should not be null")

        val innerTypesAsList = possiblyInnerType.segments()

        val indexOfParameterizedType = innerTypesAsList.indexOfFirst { innerPart -> innerPart.arguments.isNotEmpty() }
        if (indexOfParameterizedType < 0 || innerTypesAsList.size == 1) {
            writeClassBegin(asmType)
            writeGenericArguments(this, possiblyInnerType, mode)
        } else {
            val outerType = innerTypesAsList[indexOfParameterizedType]

            writeOuterClassBegin(asmType, mapType(outerType.classifier.defaultType).internalName)
            writeGenericArguments(this, outerType, mode)

            writeInnerParts(
                innerTypesAsList,
                this,
                mode,
                indexOfParameterizedType + 1
            ) // inner parts separated by `.`
        }

        writeClassEnd()
    }

    private fun hasNothingInNonContravariantPosition(irType: IrType): Boolean = with(KotlinTypeMapper) {
        typeSystem.hasNothingInNonContravariantPosition(irType)
    }

    private fun writeInnerParts(
        innerTypesAsList: List<PossiblyInnerIrType>,
        sw: JvmSignatureWriter,
        mode: TypeMappingMode,
        index: Int
    ) {
        for (innerPart in innerTypesAsList.subList(index, innerTypesAsList.size)) {
            sw.writeInnerClass(getJvmShortName(innerPart.classifier))
            writeGenericArguments(sw, innerPart, mode)
        }
    }

    // Copied from KotlinTypeMapper.writeGenericArguments.
    private fun writeGenericArguments(
        sw: JvmSignatureWriter,
        type: PossiblyInnerIrType,
        mode: TypeMappingMode
    ) {
        val classifier = type.classifier
        val parameters = classifier.typeParameters.map(IrTypeParameter::symbol)
        val arguments = type.arguments

        if ((classifier.symbol.isFunction() && arguments.size > BuiltInFunctionArity.BIG_ARITY)
            || classifier.symbol.isKFunction() || classifier.symbol.isKSuspendFunction()
        ) {
            writeGenericArguments(sw, listOf(arguments.last()), listOf(parameters.last()), mode)
            return
        }

        writeGenericArguments(sw, arguments, parameters, mode)
    }

    private fun writeGenericArguments(
        sw: JvmSignatureWriter,
        arguments: List<IrTypeArgument>,
        parameters: List<IrTypeParameterSymbol>,
        mode: TypeMappingMode
    ) {
        with(KotlinTypeMapper) {
            typeSystem.writeGenericArguments(sw, arguments, parameters, mode) { type, sw, mode ->
                mapType(type as IrType, mode, sw)
            }
        }
    }
}

private class IrTypeCheckerContextForTypeMapping(
    private val baseContext: IrTypeSystemContext,
    private val backendContext: JvmBackendContext
) : IrTypeSystemContext by baseContext, TypeSystemCommonBackendContextForTypeMapping {
    override fun TypeConstructorMarker.isTypeParameter(): Boolean {
        return this is IrTypeParameterSymbol
    }

    override fun TypeConstructorMarker.defaultType(): IrType {
        return when (this) {
            is IrClassSymbol -> owner.defaultType
            is IrTypeParameterSymbol -> owner.defaultType
            else -> error("Unsupported type constructor: $this")
        }
    }

    override fun SimpleTypeMarker.isSuspendFunction(): Boolean {
        require(this is IrSimpleType)
        return isSuspendFunctionImpl()
    }

    override fun SimpleTypeMarker.isKClass(): Boolean {
        require(this is IrSimpleType)
        return isKClassImpl()
    }

    override fun KotlinTypeMarker.isRawType(): Boolean {
        require(this is IrType)
        if (this !is IrSimpleType) return false
        return isRawTypeImpl()
    }

    override fun TypeConstructorMarker.typeWithArguments(arguments: List<KotlinTypeMarker>): IrSimpleType {
        require(this is IrClassSymbol)
        arguments.forEach {
            require(it is IrType)
        }
        @Suppress("UNCHECKED_CAST")
        return typeWith(arguments as List<IrType>)
    }

    override fun TypeParameterMarker.representativeUpperBound(): IrType {
        require(this is IrTypeParameterSymbol)
        return owner.representativeUpperBound
    }

    override fun continuationTypeConstructor(): IrClassSymbol {
        return backendContext.ir.symbols.continuationClass
    }

    override fun functionNTypeConstructor(n: Int): IrClassSymbol {
        return backendContext.referenceClass(backendContext.builtIns.getFunction(n))
    }
}
