/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.typeAttributeExtensions
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeUnsafe(): T = (this as FirResolvedTypeRef).type as T

@OptIn(ExperimentalContracts::class)
inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafe(): T? {
    contract {
        returnsNotNull() implies (this@coneTypeSafe is FirResolvedTypeRef)
    }
    return (this as? FirResolvedTypeRef)?.type as? T
}

val FirTypeRef.coneType: ConeKotlinType
    get() = coneTypeSafe()
        ?: error("Expected FirResolvedTypeRef with ConeKotlinType but was ${this::class.simpleName} ${render()}")

val FirTypeRef.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val FirTypeRef.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val FirTypeRef.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val FirTypeRef.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val FirTypeRef.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)
val FirTypeRef.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)
val FirTypeRef.isInt: Boolean get() = isBuiltinType(StandardClassIds.Int, false)
val FirTypeRef.isString: Boolean get() = isBuiltinType(StandardClassIds.String, false)
val FirTypeRef.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, false)
val FirTypeRef.isArrayType: Boolean
    get() =
        isBuiltinType(StandardClassIds.Array, false) ||
                StandardClassIds.primitiveArrayTypeByElementType.values.any { isBuiltinType(it, false) }

val FirExpression.isNullLiteral: Boolean
    get() = this is FirConstExpression<*> &&
            this.kind == ConstantValueKind.Null &&
            this.value == null &&
            this.source != null

@OptIn(ExperimentalContracts::class)
fun FirExpression.isStableSmartcast(): Boolean {
    contract {
        returns(true) implies (this@isStableSmartcast is FirSmartCastExpression)
    }
    return this is FirSmartCastExpression && this.isStable
}

private val FirTypeRef.lookupTagBasedOrNull: ConeLookupTagBasedType?
    get() = when (this) {
        is FirImplicitBuiltinTypeRef -> type
        is FirResolvedTypeRef -> type as? ConeLookupTagBasedType
        else -> null
    }

private fun FirTypeRef.isBuiltinType(classId: ClassId, isNullable: Boolean): Boolean {
    val type = this.lookupTagBasedOrNull ?: return false
    return (type as? ConeClassLikeType)?.lookupTag?.classId == classId && type.isNullable == isNullable
}

val FirTypeRef.isMarkedNullable: Boolean?
    get() = if (this is FirTypeRefWithNullability) this.isMarkedNullable else lookupTagBasedOrNull?.isMarkedNullable

val FirFunctionTypeRef.parametersCount: Int
    get() = if (receiverTypeRef != null)
        valueParameters.size + contextReceiverTypeRefs.size + 1
    else
        valueParameters.size + contextReceiverTypeRefs.size

val EXTENSION_FUNCTION_ANNOTATION = ClassId.fromString("kotlin/ExtensionFunctionType")
val INTRINSIC_CONST_EVALUATION_ANNOTATION = ClassId.fromString("kotlin/internal/IntrinsicConstEvaluation")

private fun FirAnnotation.isOfType(classId: ClassId): Boolean {
    return (annotationTypeRef as? FirResolvedTypeRef)?.let { typeRef ->
        (typeRef.type as? ConeClassLikeType)?.let {
            it.lookupTag.classId == classId
        }
    } == true
}

val FirAnnotation.isExtensionFunctionAnnotationCall: Boolean
    get() = isOfType(EXTENSION_FUNCTION_ANNOTATION)

fun List<FirAnnotation>.dropExtensionFunctionAnnotation(): List<FirAnnotation> {
    return filterNot { it.isExtensionFunctionAnnotationCall }
}

fun ConeClassLikeType.toConstKind(): ConstantValueKind<*>? = when (lookupTag.classId) {
    StandardClassIds.Byte -> ConstantValueKind.Byte
    StandardClassIds.Short -> ConstantValueKind.Short
    StandardClassIds.Int -> ConstantValueKind.Int
    StandardClassIds.Long -> ConstantValueKind.Long

    StandardClassIds.UInt -> ConstantValueKind.UnsignedInt
    StandardClassIds.ULong -> ConstantValueKind.UnsignedLong
    StandardClassIds.UShort -> ConstantValueKind.UnsignedShort
    StandardClassIds.UByte -> ConstantValueKind.UnsignedByte
    else -> null
}

fun List<FirAnnotation>.computeTypeAttributes(session: FirSession, predefined: List<ConeAttribute<*>> = emptyList()): ConeAttributes {
    if (this.isEmpty()) {
        if (predefined.isEmpty()) return ConeAttributes.Empty
        return ConeAttributes.create(predefined)
    }
    val attributes = mutableListOf<ConeAttribute<*>>()
    attributes += predefined
    val customAnnotations = mutableListOf<FirAnnotation>()
    for (annotation in this) {
        val type = annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
        when (type.lookupTag.classId) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.ExtensionFunctionType
            CompilerConeAttributes.ContextFunctionTypeParams.ANNOTATION_CLASS_ID ->
                attributes +=
                    CompilerConeAttributes.ContextFunctionTypeParams(
                        annotation.extractContextReceiversCount() ?: 0
                    )
            CompilerConeAttributes.UnsafeVariance.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.UnsafeVariance
            else -> {
                val attributeFromPlugin = session.extensionService.typeAttributeExtensions.firstNotNullOfOrNull {
                    it.extractAttributeFromAnnotation(annotation)
                }
                if (attributeFromPlugin != null) {
                    attributes += attributeFromPlugin
                } else {
                    customAnnotations += annotation
                }
            }
        }
    }
    if (customAnnotations.isNotEmpty()) {
        attributes += CustomAnnotationTypeAttribute(customAnnotations)
    }
    return ConeAttributes.create(attributes)
}

private fun FirAnnotation.extractContextReceiversCount() =
    (argumentMapping.mapping[StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME] as? FirConstExpression<*>)?.value as? Int

fun FirTypeProjection.toConeTypeProjection(): ConeTypeProjection =
    when (this) {
        is FirStarProjection -> ConeStarProjection
        is FirTypeProjectionWithVariance -> {
            val type = typeRef.coneType
            type.toTypeProjection(this.variance)
        }
        else -> error("!")
    }

private fun ConeTypeParameterType.hasNotNullUpperBound(): Boolean {
    return lookupTag.typeParameterSymbol.resolvedBounds.any {
        val boundType = it.coneType
        if (boundType is ConeTypeParameterType) {
            boundType.hasNotNullUpperBound()
        } else {
            boundType.nullability == ConeNullability.NOT_NULL
        }
    }
}

val FirTypeRef.canBeNull: Boolean
    get() = coneType.canBeNull

val ConeKotlinType.canBeNull: Boolean
    get() {
        if (isMarkedNullable) {
            return true
        }
        return when (this) {
            is ConeFlexibleType -> upperBound.canBeNull
            is ConeDefinitelyNotNullType -> false
            is ConeTypeParameterType -> this.lookupTag.typeParameterSymbol.resolvedBounds.all { it.coneType.canBeNull }
            is ConeIntersectionType -> intersectedTypes.all { it.canBeNull }
            else -> isNullable
        }
    }

val FirIntersectionTypeRef.isLeftValidForDefinitelyNotNullable
    get() = leftType.coneType.let { it is ConeTypeParameterType && it.canBeNull && !it.isMarkedNullable }

val FirIntersectionTypeRef.isRightValidForDefinitelyNotNullable get() = rightType.coneType.isAny
