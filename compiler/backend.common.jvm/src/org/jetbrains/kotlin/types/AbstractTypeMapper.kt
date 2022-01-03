/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.load.kotlin.JvmDescriptorTypeWriter
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.mapBuiltInType
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.org.objectweb.asm.Type

interface TypeMappingContext<Writer : JvmDescriptorTypeWriter<Type>> {
    val typeContext: TypeSystemCommonBackendContextForTypeMapping

    fun getClassInternalName(typeConstructor: TypeConstructorMarker): String
    fun getScriptInternalName(typeConstructor: TypeConstructorMarker): String

    // NB: The counterpart, [KotlinTypeMapper#writeGenericType], doesn't have restriction on [type]
    fun Writer.writeGenericType(type: KotlinTypeMarker, asmType: Type, mode: TypeMappingMode)
}

object AbstractTypeMapper {
    fun <Writer : JvmDescriptorTypeWriter<Type>> mapClass(context: TypeMappingContext<Writer>, typeConstructor: TypeConstructorMarker): Type {
        return with(context.typeContext) {
            when {
                typeConstructor.isClassTypeConstructor() -> {
                    mapType(context, typeConstructor.defaultType(), TypeMappingMode.CLASS_DECLARATION)
                }
                typeConstructor.isTypeParameter() -> {
                    mapType(context, typeConstructor.defaultType())
                }
                else -> error("Unknown type constructor: $typeConstructor")
            }
        }
    }

    fun <Writer : JvmDescriptorTypeWriter<Type>> mapType(
        context: TypeMappingContext<Writer>,
        type: KotlinTypeMarker,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: Writer? = null
    ): Type = context.typeContext.mapType(context, type, mode, sw)

    // NB: The counterpart, [descriptorBasedTypeSignatureMapping#mapType] doesn't have restriction on [type].
    @OptIn(ExperimentalStdlibApi::class)
    private fun <Writer : JvmDescriptorTypeWriter<Type>> TypeSystemCommonBackendContextForTypeMapping.mapType(
        context: TypeMappingContext<Writer>,
        type: KotlinTypeMarker,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: Writer? = null
    ): Type {
        if (type is SimpleTypeMarker && type.isSuspendFunction()) {
            val argumentsCount = type.argumentsCount()
            val argumentsList = type.asArgumentList()

            @Suppress("RemoveExplicitTypeArguments") // Workaround for KT-42175
            val arguments = buildList<KotlinTypeMarker> {
                for (i in 0 until (argumentsCount - 1)) {
                    this += argumentsList[i].adjustedType()
                }
                this += continuationTypeConstructor().typeWithArguments(argumentsList[argumentsCount - 1].adjustedType())
                this += nullableAnyType()
            }
            val runtimeFunctionType = functionNTypeConstructor(arguments.size - 1).typeWithArguments(arguments)
            return mapType(context, runtimeFunctionType, mode, sw)
        }

        mapBuiltInType(type, AsmTypeFactory, mode)?.let { builtInType ->
            return boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing).also { asmType ->
                with(context) { sw?.writeGenericType(type, asmType, mode) }
            }
        }

        val typeConstructor = type.typeConstructor()

        when {
            type is SimpleTypeMarker && type.isArrayOrNullableArray() -> {
                val typeArgument = type.asArgumentList()[0]
                val (variance, memberType) = when {
                    typeArgument.isStarProjection() -> Variance.OUT_VARIANCE to nullableAnyType()
                    else -> typeArgument.getVariance().toVariance() to typeArgument.getType()
                }
                require(memberType is SimpleTypeMarker)

                val arrayElementType: Type
                sw?.writeArrayType()
                if (variance == Variance.IN_VARIANCE) {
                    arrayElementType = AsmTypes.OBJECT_TYPE
                    sw?.writeClass(arrayElementType)
                } else {
                    arrayElementType = mapType(context, memberType, mode.toGenericArgumentMode(variance, ofArray = true), sw)
                }
                sw?.writeArrayEnd()
                return AsmUtil.getArrayType(arrayElementType)
            }

            type is SimpleTypeMarker && typeConstructor.isClassTypeConstructor() -> {
                if (typeConstructor.isInlineClass() && !mode.needInlineClassWrapping) {
                    val expandedType = computeExpandedTypeForInlineClass(type)
                    require(expandedType is SimpleTypeMarker?)
                    if (expandedType != null) {
                        return mapType(context, expandedType, mode.wrapInlineClassesMode(), sw)
                    }
                }

                val asmType = if (mode.isForAnnotationParameter && type.isKClass())
                    AsmTypes.JAVA_CLASS_TYPE
                else
                    Type.getObjectType(context.getClassInternalName(typeConstructor))

                with(context) { sw?.writeGenericType(type, asmType, mode) }
                return asmType
            }

            typeConstructor.isScript() -> {
                return Type.getObjectType(context.getScriptInternalName(typeConstructor))
            }

            typeConstructor.isTypeParameter() -> {
                val typeParameter = typeConstructor.asTypeParameter()
                val upperBound = typeParameter.representativeUpperBound()
                val upperBoundIsPrimitiveOrInlineClass =
                    upperBound.typeConstructor().isInlineClass() || upperBound is SimpleTypeMarker && upperBound.isPrimitiveType()
                val newType = if (upperBoundIsPrimitiveOrInlineClass && type.isNullableType())
                    upperBound.makeNullable()
                else upperBound
                return mapType(context, newType, mode, null).also { asmType ->
                    sw?.writeTypeVariable(typeParameter.getName(), asmType)
                }
            }

            type.isFlexible() -> {
                return mapType(context, type.upperBoundIfFlexible(), mode, sw)
            }

            else -> throw UnsupportedOperationException("Unknown type $type")
        }
    }

    private fun boxTypeIfNeeded(possiblyPrimitiveType: Type, needBoxedType: Boolean): Type =
        if (needBoxedType) AsmUtil.boxType(possiblyPrimitiveType) else possiblyPrimitiveType

    private fun TypeVariance.toVariance(): Variance = when (this) {
        TypeVariance.IN -> Variance.IN_VARIANCE
        TypeVariance.OUT -> Variance.OUT_VARIANCE
        TypeVariance.INV -> Variance.INVARIANT
    }
}
