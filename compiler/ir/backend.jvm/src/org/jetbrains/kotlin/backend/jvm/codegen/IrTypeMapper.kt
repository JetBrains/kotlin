/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.transformSuspendFunctionToRuntimeFunctionType
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.computeExpandedTypeForInlineClass
import org.jetbrains.kotlin.load.kotlin.mapBuiltInType
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type

class IrTypeMapper(private val context: JvmBackendContext) {
    private val typeSystem = IrTypeCheckerContext(context.irBuiltIns)

    fun classInternalName(irClass: IrClass): String =
        context.getLocalClassInfo(irClass)?.internalName
            ?: JvmCodegenUtil.sanitizeNameIfNeeded(computeInternalName(irClass), context.state.languageVersionSettings)

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
        val type = mapType(irType)
        return AsmUtil.boxPrimitiveType(type) ?: type
    }

    fun mapType(
        type: IrType,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: JvmSignatureWriter? = null
    ): Type {
        if (type !is IrSimpleType) {
            val kotlinType = type.originalKotlinType
            error("Unexpected type: $type (original Kotlin type=$kotlinType of ${kotlinType?.let { it::class }})")
        }

        // TODO: rewrite this part to produce the correct Type without the fake descriptor
        if (type.toKotlinType().isSuspendFunctionType) {
            return context.state.typeMapper.mapType(
                transformSuspendFunctionToRuntimeFunctionType(type.toKotlinType(), isReleaseCoroutines = true), sw, mode
            )
        }

        with(typeSystem) {
            mapBuiltInType(type, AsmTypeFactory, mode)
        }?.let { builtInType ->
            return boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing).also { asmType ->
                sw?.writeGenericType(type, asmType, mode)
            }
        }

        val classifier = type.classifierOrNull?.owner

        when {
            type.isArray() || type.isNullableArray() -> {
                val typeArgument = type.arguments.single()
                val (variance, memberType) = when (typeArgument) {
                    is IrTypeProjection -> Pair(typeArgument.variance, typeArgument.type)
                    is IrStarProjection -> Pair(Variance.OUT_VARIANCE, context.irBuiltIns.anyNType)
                    else -> error("Unsupported type argument: $typeArgument")
                }

                val arrayElementType: Type
                sw?.writeArrayType()
                if (variance == Variance.IN_VARIANCE) {
                    arrayElementType = AsmTypes.OBJECT_TYPE
                    sw?.writeClass(arrayElementType)
                } else {
                    arrayElementType = mapType(memberType, mode.toGenericArgumentMode(variance), sw)
                }
                sw?.writeArrayEnd()

                return AsmUtil.getArrayType(arrayElementType)
            }

            classifier is IrClass -> {
                if (classifier.isInline && !mode.needInlineClassWrapping) {
                    val expandedType = typeSystem.computeExpandedTypeForInlineClass(type) as IrType?
                    if (expandedType != null) {
                        return mapType(expandedType, mode.wrapInlineClassesMode(), sw)
                    }
                }

                val asmType =
                    if (mode.isForAnnotationParameter && type.isKClass()) AsmTypes.JAVA_CLASS_TYPE
                    else Type.getObjectType(classInternalName(classifier))

                sw?.writeGenericType(type, asmType, mode)

                return asmType
            }

            classifier is IrTypeParameter -> {
                return mapType(classifier.representativeUpperBound, mode, null).also { asmType ->
                    sw?.writeTypeVariable(classifier.name, asmType)
                }
            }

            else -> throw UnsupportedOperationException("Unknown type $type")
        }
    }

    private fun computeInternalName(klass: IrClass): String {
        val className = SpecialNames.safeIdentifier(klass.name).identifier
        return when (val parent = klass.parent) {
            is IrPackageFragment -> {
                val fqName = parent.fqName
                val prefix = if (fqName.isRoot) "" else fqName.asString().replace('.', '/') + "/"
                prefix + className
            }
            is IrClass -> {
                computeInternalName(parent) + "$" + className
            }
            else -> error(
                "Local class should have its name computed in InventNamesForLocalClasses: ${klass.fqNameWhenAvailable}\n" +
                        "Ensure that any lowering that transforms elements with local class info (classes, function references, " +
                        "IrTypeOperatorCall for SAM conversions) invokes `copyAttributes` on the transformed element."
            )
        }
    }

    // Copied from KotlinTypeMapper.writeGenericType.
    private fun JvmSignatureWriter.writeGenericType(type: IrSimpleType, asmType: Type, mode: TypeMappingMode) {
        if (skipGenericSignature() || hasNothingInNonContravariantPosition(type) || type.arguments.isEmpty()) {
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

        // TODO: get rid of descriptor here
        val classDescriptor = classifier.descriptor
        if (classDescriptor is FunctionClassDescriptor) {
            if (classDescriptor.hasBigArity ||
                classDescriptor.functionKind == FunctionClassDescriptor.Kind.KFunction ||
                classDescriptor.functionKind == FunctionClassDescriptor.Kind.KSuspendFunction
            ) {
                writeGenericArguments(sw, listOf(arguments.last()), listOf(parameters.last()), mode)
                return
            }
        }

        writeGenericArguments(sw, arguments, parameters, mode)
    }

    private fun writeGenericArguments(
        sw: JvmSignatureWriter,
        arguments: List<IrTypeArgument>,
        parameters: List<IrTypeParameterSymbol>,
        mode: TypeMappingMode
    ) = with(KotlinTypeMapper) {
        typeSystem.writeGenericArguments(sw, arguments, parameters, mode) { type, sw, mode ->
            mapType(type as IrType, mode, sw)
        }
    }

    private fun boxTypeIfNeeded(possiblyPrimitiveType: Type, needBoxedType: Boolean): Type =
        if (needBoxedType) AsmUtil.boxType(possiblyPrimitiveType) else possiblyPrimitiveType
}
