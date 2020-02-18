/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext

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