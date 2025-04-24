/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

object ConeTypeUnion {
    fun unionTypes(
        context: ConeInferenceContext,
        types: Collection<ConeKotlinType>,
        commonSuper: KotlinTypeMarker? = null
    ): ConeKotlinType {
        when (types.size) {
            0 -> error("Expected some types")
            1 -> return types.single()
        }

        val inputTypes = mutableListOf<ConeKotlinType>().apply {
            for (inputType in types) {
                if (inputType is ConeUnionType) {
                    addAll(inputType.unionTypes)
                } else {
                    add(inputType)
                }
            }
        }

        if (inputTypes.any { it is ConeFlexibleType } && inputTypes.none { it.isRaw() || it is ConeDynamicType }) {
            TODO()
        }

        val isResultNotNullable = with(context) {
            inputTypes.any { !it.isNullableType() }
        }
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
        return resultList.singleOrNull() ?: ConeUnionType(resultList, commonSuper as ConeKotlinType)

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
}