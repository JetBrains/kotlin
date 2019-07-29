/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeIntersectionTypeConstructor.IntersectionStatus
import org.jetbrains.kotlin.types.AbstractTypeChecker
import java.util.*
import kotlin.collections.LinkedHashSet

object ConeTypeIntersector {
    fun intersectTypes(context: ConeTypeContext, types: List<ConeKotlinType>): ConeKotlinType {
        require(context is ConeInferenceContext)
        return intersectTypes(context, types, types.map { IntersectionStatus.FROM_INFERENCE })
    }

    private fun intersectTypes(
        context: ConeInferenceContext,
        types: List<ConeKotlinType>,
        intersectionStatus: List<IntersectionStatus>
    ): ConeKotlinType {
        assert(types.size == intersectionStatus.size)

        when (types.size) {
            0 -> error("Expected some types")
            1 -> return types.single()
        }

        val inputTypes = mutableListOf<ConeKotlinType>()
        val statusMap = mutableMapOf<ConeKotlinType, IntersectionStatus>()
        flatIntersectionTypes(types, intersectionStatus, inputTypes, statusMap)

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
        val resultNullability = inputTypes.fold(ResultNullability.START) { nullability, nextType ->
            nullability.combine(nextType.type, context)
        }

        val inputTypesWithCorrectNullability = inputTypes.mapTo(LinkedHashSet()) {
            if (resultNullability == ResultNullability.NOT_NULL) with(context) {
                val resultType = it.makeDefinitelyNotNullOrNotNull() as ConeKotlinType
                statusMap[resultType] = statusMap.remove(it)!!
                resultType
            } else it
        }

        return intersectTypesWithoutIntersectionType(context, inputTypesWithCorrectNullability, statusMap)
    }

    private fun intersectTypesWithoutIntersectionType(
        context: ConeTypeContext,
        inputTypes: Set<ConeKotlinType>,
        statusMap: MutableMap<ConeKotlinType, IntersectionStatus>
    ): ConeKotlinType {
        if (inputTypes.size == 1) return inputTypes.single().type

        // Any and Nothing should leave
        // Note that duplicates should be dropped because we have Set here.
        val errorMessage = { "This collections cannot be empty! input types: ${inputTypes.joinToString()}" }

        val filteredEqualTypes = filterTypes(inputTypes) { lower, upper ->
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
            isStrictSupertype(context, lower, upper) && statusMap[lower]!! <= statusMap[upper]!!
        }
        assert(filteredEqualTypes.isNotEmpty(), errorMessage)

        // TODO
        // IntegerLiteralTypeConstructor.findIntersectionType(filteredEqualTypes)?.let { return it }

        val filteredSuperAndEqualTypes = filterTypes(filteredEqualTypes) { a, b ->
            AbstractTypeChecker.equalTypes(context, a, b)
        }
        assert(filteredSuperAndEqualTypes.isNotEmpty(), errorMessage)

        if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

        val constructor = ConeIntersectionTypeConstructor(
            filteredSuperAndEqualTypes,
            filteredSuperAndEqualTypes.associateWithTo(mutableMapOf()) { statusMap[it]!! }
        )
        return ConeIntersectionType(constructor)
    }

    private fun filterTypes(
        inputTypes: Collection<ConeKotlinType>,
        predicate: (lower: ConeKotlinType, upper: ConeKotlinType) -> Boolean
    ): List<ConeKotlinType> {
        val filteredTypes = ArrayList(inputTypes)
        val iterator = filteredTypes.iterator()
        while (iterator.hasNext()) {
            val upper = iterator.next()
            val shouldFilter = filteredTypes.any { lower -> lower !== upper && predicate(lower, upper) }

            if (shouldFilter) iterator.remove()
        }
        return filteredTypes
    }

    private fun isStrictSupertype(context: ConeTypeContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
        return with(AbstractTypeChecker) {
            isSubtypeOf(context, subtype, supertype) && !isSubtypeOf(context, supertype, subtype)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun MutableMap<ConeKotlinType, IntersectionStatus>.updateStatus(type: ConeKotlinType, status: IntersectionStatus) {
        // We should keep status FROM_SMARTCAST for correct diagnostic reporting
        val existingStatus = get(type)
        if (existingStatus == null) {
            put(type, status)
        } else {
            put(type, minOf(status, existingStatus))
        }
    }

    private fun flatIntersectionTypes(
        inputTypes: List<ConeKotlinType>,
        intersectionStatus: List<IntersectionStatus>,
        typeCollector: MutableList<ConeKotlinType>,
        statusMap: MutableMap<ConeKotlinType, IntersectionStatus>
    ) {
        for ((inputType, status) in inputTypes.zip(intersectionStatus)) {
            if (inputType is ConeIntersectionType) {
                for (type in inputType.intersectedTypes) {
                    typeCollector += type
                    statusMap.updateStatus(type, status)
                }
            } else {
                typeCollector += inputType
                statusMap.updateStatus(inputType, status)
            }
        }
    }

    private enum class ResultNullability {
        START {
            override fun combine(nextType: ConeKotlinType, context: ConeTypeContext): ResultNullability =
                nextType.resultNullability(context)
        },
        ACCEPT_NULL {
            override fun combine(nextType: ConeKotlinType, context: ConeTypeContext): ResultNullability =
                nextType.resultNullability(context)
        },
        // example: type parameter without not-null supertype
        UNKNOWN {
            override fun combine(nextType: ConeKotlinType, context: ConeTypeContext): ResultNullability =
                nextType.resultNullability(context).let {
                    if (it == ACCEPT_NULL) this else it
                }
        },
        NOT_NULL {
            override fun combine(nextType: ConeKotlinType, context: ConeTypeContext): ResultNullability = this
        };

        abstract fun combine(nextType: ConeKotlinType, context: ConeTypeContext): ResultNullability

        protected fun ConeKotlinType.resultNullability(context: ConeTypeContext): ResultNullability =
            when {
                isMarkedNullable -> ACCEPT_NULL
                ConeNullabilityChecker.isSubtypeOfAny(context, this) -> NOT_NULL
                else -> UNKNOWN
            }
    }
}