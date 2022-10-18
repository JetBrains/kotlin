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
        val isNullable = inputTypes.all { it.isNullable(context) }
        val withNullability = inputTypes.mapTo(LinkedHashSet()) {
            if (!isNullable)
                it.makeConeTypeDefinitelyNotNullOrNotNull(context)
            else it
        }
        if (withNullability.size == 1) return withNullability.single()

        /*
         * Here we drop types from intersection set for cases like that:
         *
         * interface A
         * interface B : A
         *
         * type = (A & B & ...)
         *
         * We want to drop A from that set, because it's useless for type checking. But in case if
         *   A came from inference and B came from smartcast we want to safe both types in intersection
         */
        val resultList = withNullability.toMutableList()
        resultList.removeIfAny { it, other -> other.isStrictSubtypeOf(context, it) }
        assert(resultList.isNotEmpty()) { "no types left after removing strict supertypes: ${inputTypes.joinToString()}" }

        ConeIntegerLiteralIntersector.findCommonIntersectionType(resultList)?.let { return it }

        /*
         * For the case like it(ft(String..String?), String?), where ft(String..String?) == String?, we prefer to _keep_ flexible type.
         * When a == b, the former, i.e., the one in the list will be filtered out, and the other one will remain.
         * So, here, we sort the interim list such that flexible types appear later.
         */
        resultList.sortWith { p0, p1 ->
            when {
                p0 is ConeFlexibleType && p1 is ConeFlexibleType -> 0
                p0 is ConeFlexibleType -> 1
                p1 is ConeFlexibleType -> -1
                else -> 0
            }
        }
        resultList.removeIfAny { it, other -> AbstractTypeChecker.equalTypes(context, it, other) }
        assert(resultList.isNotEmpty()) { "no types left after removing equal types: ${inputTypes.joinToString()}" }
        return resultList.singleOrNull() ?: ConeIntersectionType(resultList)
    }

    private fun MutableCollection<ConeKotlinType>.removeIfAny(
        predicate: (it: ConeKotlinType, other: ConeKotlinType) -> Boolean
    ) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if ((it is ConeErrorType && size > 1) || any { other -> other !== it && predicate(it, other) }) {
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
