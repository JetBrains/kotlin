/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.load.kotlin.JvmDescriptorTypeWriter
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
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
    fun <Writer : JvmDescriptorTypeWriter<Type>> mapClass(
        context: TypeMappingContext<Writer>,
        typeConstructor: TypeConstructorMarker,
        materialized: Boolean
    ): Type {
        return with(context.typeContext) {
            when {
                typeConstructor.isClassTypeConstructor() -> {
                    mapType(context, typeConstructor.defaultType(), TypeMappingMode.CLASS_DECLARATION, materialized = materialized)
                }
                typeConstructor.isTypeParameter() -> {
                    mapType(context, typeConstructor.defaultType(), materialized = materialized)
                }
                else -> error("Unknown type constructor: $typeConstructor")
            }
        }
    }

    fun <Writer : JvmDescriptorTypeWriter<Type>> mapType(
        context: TypeMappingContext<Writer>,
        type: KotlinTypeMarker,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: Writer? = null,
        materialized: Boolean = true,
    ): Type = context.typeContext.mapType(context, type, mode, sw, materialized)

    // NB: The counterpart, [descriptorBasedTypeSignatureMapping#mapType] doesn't have restriction on [type].
    private fun <Writer : JvmDescriptorTypeWriter<Type>> TypeSystemCommonBackendContextForTypeMapping.mapType(
        context: TypeMappingContext<Writer>,
        type: KotlinTypeMarker,
        mode: TypeMappingMode,
        sw: Writer?,
        materialized: Boolean,
    ): Type {
        if (type.isError()) {
            val name = type.getNameForErrorType() ?: NON_EXISTENT_CLASS_NAME
            val jvmType = Type.getObjectType(name)
            with(context) { sw?.writeGenericType(type, jvmType, mode) }
            return jvmType
        }

        val typeConstructor = type.typeConstructor()

        if (type is SimpleTypeMarker) {
            val builtInType = mapBuiltInType(type, AsmTypeFactory, mode)
            if (builtInType != null) {
                val asmType = boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing)
                with(context) { sw?.writeGenericType(type, asmType, mode) }
                return asmType
            }

            if (type.isSuspendFunction()) {
                return mapSuspendFunctionType(type, context, mode, sw, materialized)
            }

            if (type.isArrayOrNullableArray()) {
                return mapArrayType(type, sw, context, mode, materialized)
            }

            if (typeConstructor.isClassTypeConstructor()) {
                return mapClassType(typeConstructor, mode, type, context, sw, materialized)
            }
        }

        return when {
            typeConstructor.isTypeParameter() -> {
                val typeParameter = typeConstructor.asTypeParameter()
                val upperBound = typeParameter.representativeUpperBound()
                val upperBoundIsPrimitiveOrInlineClass =
                    upperBound.typeConstructor().isInlineClass() || upperBound is SimpleTypeMarker && upperBound.isPrimitiveType()
                val newType = if (upperBoundIsPrimitiveOrInlineClass && type.isNullableType())
                    upperBound.makeNullable()
                else upperBound

                val asmType = mapType(context, newType, mode, null, materialized)
                sw?.writeTypeVariable(typeParameter.getName(), asmType)
                asmType
            }

            type.isFlexible() -> {
                mapType(context, type.upperBoundIfFlexible(), mode, sw, materialized)
            }

            type is DefinitelyNotNullTypeMarker ->
                mapType(context, type.original(), mode, sw, materialized)

            typeConstructor.isScript() ->
                Type.getObjectType(context.getScriptInternalName(typeConstructor)).let {
                    sw?.writeClass(it)
                    it
                }

            else ->
                throw UnsupportedOperationException("Unknown type $type")
        }
    }

    private fun <Writer : JvmDescriptorTypeWriter<Type>> TypeSystemCommonBackendContextForTypeMapping.mapSuspendFunctionType(
        type: SimpleTypeMarker,
        context: TypeMappingContext<Writer>,
        mode: TypeMappingMode,
        sw: Writer?,
        materialized: Boolean,
    ): Type {
        val argumentsCount = type.argumentsCount()
        val argumentsList = type.asArgumentList()
        val arguments = buildList {
            for (i in 0 until (argumentsCount - 1)) {
                this += argumentsList[i].adjustedType()
            }
            this += continuationTypeConstructor().typeWithArguments(argumentsList[argumentsCount - 1].adjustedType())
            this += nullableAnyType()
        }
        val runtimeFunctionType = functionNTypeConstructor(arguments.size - 1).typeWithArguments(arguments)
        return mapType(context, runtimeFunctionType, mode, sw, materialized)
    }

    private fun <Writer : JvmDescriptorTypeWriter<Type>> TypeSystemCommonBackendContextForTypeMapping.mapArrayType(
        type: SimpleTypeMarker,
        sw: Writer?,
        context: TypeMappingContext<Writer>,
        mode: TypeMappingMode,
        materialized: Boolean
    ): Type {
        val typeArgument = type.asArgumentList()[0]
        val variance: Variance
        val memberType: KotlinTypeMarker

        if (typeArgument.isStarProjection()) {
            variance = Variance.OUT_VARIANCE
            memberType = nullableAnyType()
        } else {
            variance = typeArgument.getVariance().toVariance()
            memberType = typeArgument.getType()
        }

        val arrayElementType: Type
        sw?.writeArrayType()
        if (variance == Variance.IN_VARIANCE) {
            arrayElementType = AsmTypes.OBJECT_TYPE
            sw?.writeClass(arrayElementType)
        } else {
            arrayElementType = mapType(context, memberType, mode.toGenericArgumentMode(variance, ofArray = true), sw, materialized)
        }
        sw?.writeArrayEnd()
        return AsmUtil.getArrayType(arrayElementType)
    }

    private fun <Writer : JvmDescriptorTypeWriter<Type>> TypeSystemCommonBackendContextForTypeMapping.mapClassType(
        typeConstructor: TypeConstructorMarker,
        mode: TypeMappingMode,
        type: SimpleTypeMarker,
        context: TypeMappingContext<Writer>,
        sw: Writer?,
        materialized: Boolean
    ): Type {
        if (typeConstructor.isInlineClass() && !mode.needInlineClassWrapping) {
            val expandedType = computeExpandedTypeForInlineClass(type)
            require(expandedType is SimpleTypeMarker?)
            if (expandedType != null) {
                return mapType(context, expandedType, mode.wrapInlineClassesMode(), sw, materialized)
            }
        }

        val asmType = if (mode.isForAnnotationParameter && type.isKClass())
            AsmTypes.JAVA_CLASS_TYPE
        else
            Type.getObjectType(context.getClassInternalName(typeConstructor))

        with(context) { sw?.writeGenericType(type, asmType, mode) }
        return asmType
    }

    private fun boxTypeIfNeeded(possiblyPrimitiveType: Type, needBoxedType: Boolean): Type =
        if (needBoxedType) AsmUtil.boxType(possiblyPrimitiveType) else possiblyPrimitiveType

    private fun TypeVariance.toVariance(): Variance = when (this) {
        TypeVariance.IN -> Variance.IN_VARIANCE
        TypeVariance.OUT -> Variance.OUT_VARIANCE
        TypeVariance.INV -> Variance.INVARIANT
    }

    fun <Writer : JvmDescriptorTypeWriter<Type>> isPrimitiveBacked(
        context: TypeMappingContext<Writer>,
        type: KotlinTypeMarker
    ): Boolean = context.typeContext.isPrimitiveBacked(type)

    private fun TypeSystemCommonBackendContext.isPrimitiveBacked(type: KotlinTypeMarker): Boolean =
        !type.isNullableType() &&
                (type is SimpleTypeMarker && type.isPrimitiveType() ||
                        type.typeConstructor().getValueClassProperties()?.singleOrNull()?.let { isPrimitiveBacked(it.second) } == true)
}
