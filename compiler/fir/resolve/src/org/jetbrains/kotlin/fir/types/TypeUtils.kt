/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.Variance

object ConeNullabilityChecker {
    fun isSubtypeOfAny(context: ConeTypeContext, type: ConeKotlinType): Boolean {
        val actualType = with(context) { type.lowerBoundIfFlexible() }
        return with(AbstractNullabilityChecker) {
            context.newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
                .hasNotNullSupertype(actualType, AbstractTypeCheckerContext.SupertypesPolicy.LowerIfFlexible)
        }
    }
}

fun ConeInferenceContext.commonSuperTypeOrNull(types: List<ConeKotlinType>): ConeKotlinType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> with(NewCommonSuperTypeCalculator) {
            commonSuperType(types) as ConeKotlinType
        }
    }
}

fun ConeInferenceContext.intersectTypesOrNull(types: List<ConeKotlinType>): ConeKotlinType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> ConeTypeIntersector.intersectTypes(this, types)
    }
}

fun ConeDefinitelyNotNullType.Companion.create(original: ConeKotlinType): ConeDefinitelyNotNullType? {
    return when {
        original is ConeDefinitelyNotNullType -> original
        makesSenseToBeDefinitelyNotNull(original) ->
            ConeDefinitelyNotNullType(original.lowerBoundIfFlexible())
        else -> null
    }
}

fun makesSenseToBeDefinitelyNotNull(type: ConeKotlinType): Boolean =
    type.canHaveUndefinedNullability() // TODO: also check nullability

fun ConeKotlinType.canHaveUndefinedNullability(): Boolean {
    return when (this) {
        is ConeTypeVariableType,
        is ConeCapturedType
        -> true
        is ConeTypeParameterType -> type.isMarkedNullable || !hasNotNullUpperBound()
        else -> false
    }
}

private fun ConeTypeParameterType.hasNotNullUpperBound(): Boolean {
    return lookupTag.typeParameterSymbol.fir.bounds.any {
        val boundType = it.coneTypeUnsafe<ConeKotlinType>()
        if (boundType is ConeTypeParameterType) {
            boundType.hasNotNullUpperBound()
        } else {
            boundType.nullability == ConeNullability.NOT_NULL
        }
    }
}

fun ConeKotlinType.makeConeTypeDefinitelyNotNullOrNotNull(): ConeKotlinType {
    if (this is ConeIntersectionType) {
        return ConeIntersectionType(intersectedTypes.map { it.makeConeTypeDefinitelyNotNullOrNotNull() })
    }
    return ConeDefinitelyNotNullType.create(this) ?: this.withNullability(ConeNullability.NOT_NULL)
}

fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, arguments, nullability.isNullable) as T
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType.create(original.withArguments(arguments))!! as T
        else -> error("Not supported: $this: ${this.render()}")
    }
}

fun ConeTypeContext.hasNullableSuperType(type: ConeKotlinType): Boolean {
    if (type is ConeClassLikeType) return false

    if (type !is ConeLookupTagBasedType) return false // TODO?
    val symbol = type.lookupTag.toSymbol(session) ?: return false // TODO?!
    for (superType in symbol.supertypes()) {
        if (superType.isNullableType()) return true
    }
//
//    for (KotlinType supertype : getImmediateSupertypes(type)) {
//        if (isNullableType(supertype)) return true;
//    }

    return false
}

fun <T : ConeKotlinType> T.withNullability(nullability: ConeNullability, typeContext: ConeInferenceContext? = null): T {
    if (this.nullability == nullability) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable)
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable)
        is ConeFlexibleType -> {
            if (nullability == ConeNullability.UNKNOWN) {
                if (lowerBound.nullability != upperBound.nullability || lowerBound.nullability == ConeNullability.UNKNOWN) {
                    return this
                }
            }
            coneFlexibleOrSimpleType(typeContext, lowerBound.withNullability(nullability), upperBound.withNullability(nullability))
        }
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, lookupTag)
        is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, nullability, constructor)
        is ConeIntersectionType -> when (nullability) {
            ConeNullability.NULLABLE -> this.mapTypes {
                it.withNullability(nullability)
            }
            ConeNullability.UNKNOWN -> this // TODO: is that correct?
            ConeNullability.NOT_NULL -> this
        }
        is ConeStubType -> ConeStubType(variable, nullability)
        is ConeDefinitelyNotNullType -> when (nullability) {
            ConeNullability.NOT_NULL -> this
            ConeNullability.NULLABLE -> original.withNullability(nullability)
            ConeNullability.UNKNOWN -> original.withNullability(nullability)
        }
        is ConeIntegerLiteralType -> ConeIntegerLiteralTypeImpl(value, isUnsigned, nullability)
        else -> error("sealed: ${this::class}")
    } as T
}

fun coneFlexibleOrSimpleType(
    typeContext: ConeInferenceContext?,
    lowerBound: ConeKotlinType,
    upperBound: ConeKotlinType,
): ConeKotlinType {
    if (lowerBound is ConeFlexibleType) {
        return coneFlexibleOrSimpleType(typeContext, lowerBound.lowerBound, upperBound)
    }
    if (upperBound is ConeFlexibleType) {
        return coneFlexibleOrSimpleType(typeContext, lowerBound, upperBound.upperBound)
    }
    return when {
        typeContext != null && AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, lowerBound, upperBound) -> {
            lowerBound
        }
        typeContext == null && lowerBound == upperBound -> {
            lowerBound
        }
        else -> {
            ConeFlexibleType(lowerBound, upperBound)
        }
    }
}

fun ConeKotlinType.toTypeProjection(variance: Variance): ConeTypeProjection =
    when (variance) {
        Variance.INVARIANT -> this
        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(this)
        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(this)
    }

fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean,
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this, typeArguments, isNullable)
}

fun ConeClassifierLookupTag.constructType(typeArguments: Array<out ConeTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, isNullable)
        else -> error("! ${this::class}")
    }
}
