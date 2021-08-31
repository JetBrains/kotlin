/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.TypeCheckerState
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
        returns(true) implies (this@isStableSmartcast is FirExpressionWithSmartcast)
    }
    return this is FirExpressionWithSmartcast && this.isStable
}

private val FirTypeRef.classLikeTypeOrNull: ConeClassLikeType?
    get() = when (this) {
        is FirImplicitBuiltinTypeRef -> type
        is FirResolvedTypeRef -> type as? ConeClassLikeType
        else -> null
    }

private fun FirTypeRef.isBuiltinType(classId: ClassId, isNullable: Boolean): Boolean {
    val type = this.classLikeTypeOrNull ?: return false
    return type.lookupTag.classId == classId && type.isNullable == isNullable
}

val FirTypeRef.isMarkedNullable: Boolean?
    get() = classLikeTypeOrNull?.isMarkedNullable

val FirFunctionTypeRef.parametersCount: Int
    get() = if (receiverTypeRef != null)
        valueParameters.size + 1
    else
        valueParameters.size

val EXTENSION_FUNCTION_ANNOTATION = ClassId.fromString("kotlin/ExtensionFunctionType")

val FirAnnotationCall.isExtensionFunctionAnnotationCall: Boolean
    get() = (this as? FirAnnotationCall)?.let { annotationCall ->
        (annotationCall.annotationTypeRef as? FirResolvedTypeRef)?.let { typeRef ->
            (typeRef.type as? ConeClassLikeType)?.let {
                it.lookupTag.classId == EXTENSION_FUNCTION_ANNOTATION
            }
        }
    } == true


fun List<FirAnnotationCall>.dropExtensionFunctionAnnotation(): List<FirAnnotationCall> {
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

fun List<FirAnnotationCall>.computeTypeAttributes(
    additionalProcessor: MutableList<ConeAttribute<*>>.(ClassId) -> Unit = {}
): ConeAttributes {
    if (this.isEmpty()) return ConeAttributes.Empty
    val attributes = mutableListOf<ConeAttribute<*>>()
    val customAnnotations = mutableListOf<FirAnnotationCall>()
    for (annotation in this) {
        val type = annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
        when (val classId = type.lookupTag.classId) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.ExtensionFunctionType
            CompilerConeAttributes.UnsafeVariance.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.UnsafeVariance
            else -> {
                val annotationAttributes = mutableListOf<ConeAttribute<*>>()
                additionalProcessor.invoke(annotationAttributes, classId)
                if (annotationAttributes.isEmpty()) {
                    customAnnotations += annotation
                } else {
                    attributes += annotationAttributes
                }
            }
        }
    }
    if (customAnnotations.isNotEmpty()) {
        attributes += CustomAnnotationTypeAttribute(customAnnotations)
    }
    return ConeAttributes.create(attributes)
}

fun FirTypeProjection.toConeTypeProjection(): ConeTypeProjection =
    when (this) {
        is FirStarProjection -> ConeStarProjection
        is FirTypeProjectionWithVariance -> {
            val type = typeRef.coneType
            type.toTypeProjection(this.variance)
        }
        else -> error("!")
    }

fun TypeCheckerState.makesSenseToBeDefinitelyNotNull(
    type: ConeKotlinType,
    useCorrectedNullabilityForFlexibleTypeParameters: Boolean
): Boolean {
    if (!type.canHaveUndefinedNullability()) return false

    // Replacing `useCorrectedNullabilityForFlexibleTypeParameters` with true for all call-sites seems to be correct
    // But it seems that it should be a new feature: KT-28785 would be automatically fixed then
    // (see the tests org.jetbrains.kotlin.spec.checkers.DiagnosticsTestSpecGenerated.NotLinked.Dfa.Pos.test12/13)
    // So it should be a language feature, but it's hard correctly identify language version settings for all call sites
    // Thus, we have non-trivial value at org.jetbrains.kotlin.load.java.typeEnhancement.JavaTypeEnhancement.notNullTypeParameter
    // that run under related language-feature only
    if (useCorrectedNullabilityForFlexibleTypeParameters && type is ConeTypeParameterType) {
        // Effectively checks if the type is flexible or has nullable bound
        return with(typeSystemContext) {
            type.isNullableType()
        }
    }

    // Actually, this code should work for type parameters as well, but it breaks some cases
    // See KT-40114

    return !AbstractNullabilityChecker.isSubtypeOfAny(this, type)
}

fun ConeKotlinType.canHaveUndefinedNullability(): Boolean {
    return when (this) {
        is ConeTypeVariableType,
        is ConeCapturedType,
        is ConeTypeParameterType
        -> true
        else -> false
    }
}

private fun ConeTypeParameterType.hasNotNullUpperBound(): Boolean {
    return lookupTag.typeParameterSymbol.fir.bounds.any {
        val boundType = it.coneType
        if (boundType is ConeTypeParameterType) {
            boundType.hasNotNullUpperBound()
        } else {
            boundType.nullability == ConeNullability.NOT_NULL
        }
    }
}

val FirTypeRef.canBeNull: Boolean
    // TODO: replace with coneType (for some reason, implicit type still can arise here)
    get() = coneTypeSafe<ConeKotlinType>()?.canBeNull == true

val ConeKotlinType.canBeNull: Boolean
    get() {
        if (isMarkedNullable) {
            return true
        }
        return when (this) {
            is ConeFlexibleType -> upperBound.canBeNull
            is ConeDefinitelyNotNullType -> false
            is ConeTypeParameterType -> this.lookupTag.typeParameterSymbol.fir.bounds.all { it.coneType.canBeNull }
            is ConeIntersectionType -> intersectedTypes.all { it.canBeNull }
            else -> isNullable
        }
    }

val ConeKotlinType?.functionTypeKind: FunctionClassKind?
    get() {
        val classId = (this as? ConeClassLikeType)?.lookupTag?.classId ?: return null
        return FunctionClassKind.getFunctionalClassKind(
            classId.shortClassName.asString(), classId.packageFqName
        )
    }

val FirResolvedTypeRef.functionTypeKind: FunctionClassKind?
    get() = type.functionTypeKind
