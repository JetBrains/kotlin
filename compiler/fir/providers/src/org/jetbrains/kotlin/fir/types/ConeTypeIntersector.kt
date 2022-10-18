/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.AbstractTypeChecker

object ConeTypeIntersector {
    fun intersectTypes(
        context: ConeInferenceContext,
        types: List<ConeKotlinType>
    ): ConeKotlinType {
        when (types.size) {
            0 -> error("Expected some types")
            1 -> return types.single()
        }

        val inputTypes = mutableListOf<ConeKotlinType>().apply {
            for (inputType in types) {
                if (inputType is ConeIntersectionType) {
                    addAll(inputType.intersectedTypes)
                } else {
                    add(inputType)
                }
            }
        }

        if (inputTypes.any { it is ConeFlexibleType }) {
            // (A..B) & C = (A & C)..(B & C)
            val lowerBound = intersectTypes(context, inputTypes.map { it.lowerBoundIfFlexible() })
            val upperBound = intersectTypes(context, inputTypes.map { it.upperBoundIfFlexible() })
            // Special case - if C is `Nothing?`, then the result is `Nothing!`; but if it is non-null,
            // then this code is unreachable, so it's more useful to do resolution/diagnostics
            // under the assumption that it is purely nullable.
            return if (lowerBound.isNothing) upperBound else coneFlexibleOrSimpleType(context, lowerBound, upperBound)
        }

        /**
         * resultNullability. Value description:
         * ACCEPT_NULL means that all types marked nullable
         *
         * NOT_NULL means that there is one type which is subtype of Any => all types can be made definitely not null,
         * making types definitely not null (not just not null) makes sense when we have intersection of type parameters like {T!! & S}
         *
         * UNKNOWN means, that we do not know, i.e. more precisely, all singleClassifier types marked nullable if any,
         * and other types is captured types or type parameters without not-null upper bound. Example: `String? & T` such types we should leave as is.
         */
        val isResultNotNullable = inputTypes.any { !it.isNullable(context) }
        val inputTypesMadeNotNullIfNeeded = inputTypes.mapTo(LinkedHashSet()) {
            if (isResultNotNullable) it.makeConeTypeDefinitelyNotNullOrNotNull(context) else it
        }
        if (inputTypesMadeNotNullIfNeeded.size == 1) return inputTypesMadeNotNullIfNeeded.single()

        /*
         * Here we drop types from intersection set for cases like that:
         *
         * interface A
         * interface B : A
         *
         * type = (A & B & ...)
         *
         * We want to drop A from that set, because it's useless for type checking. But in case if
         *   A came from inference and B came from smartcast we want to save both types in intersection
         */
        val resultList = inputTypesMadeNotNullIfNeeded.toMutableList()
        resultList.removeIfNonSingleErrorOrInRelation { candidate, other -> other.isStrictSubtypeOf(context, candidate) }
        assert(resultList.isNotEmpty()) { "no types left after removing strict supertypes: ${inputTypes.joinToString()}" }

        ConeIntegerLiteralIntersector.findCommonIntersectionType(resultList)?.let { return it }

        resultList.removeIfNonSingleErrorOrInRelation { candidate, other -> AbstractTypeChecker.equalTypes(context, candidate, other) }
        assert(resultList.isNotEmpty()) { "no types left after removing equal types: ${inputTypes.joinToString()}" }
        return resultList.singleOrNull() ?: ConeIntersectionType(resultList)
    }

    private fun MutableCollection<ConeKotlinType>.removeIfNonSingleErrorOrInRelation(
        predicate: (candidate: ConeKotlinType, other: ConeKotlinType) -> Boolean
    ) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (candidate is ConeErrorType && size > 1 ||
                any { other -> other !== candidate && predicate(candidate, other) }
            ) {
                iterator.remove()
            }
        }
    }

    private fun ConeKotlinType.isStrictSubtypeOf(context: ConeTypeContext, supertype: ConeKotlinType): Boolean =
        AbstractTypeChecker.isSubtypeOf(context, this, supertype) && !AbstractTypeChecker.isSubtypeOf(context, supertype, this)

    private fun ConeKotlinType.isNullable(context: ConeTypeContext): Boolean =
        when {
            isMarkedNullable -> true
            this is ConeFlexibleType -> upperBound.isNullable(context)
            else -> !ConeNullabilityChecker.isSubtypeOfAny(context, this)
        }
}
