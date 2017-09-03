/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*
import java.util.*
import kotlin.collections.LinkedHashSet

fun intersectWrappedTypes(types: Collection<KotlinType>) = intersectTypes(types.map { it.unwrap() })


fun intersectTypes(types: List<SimpleType>) = intersectTypes(types as List<UnwrappedType>) as SimpleType

fun intersectTypes(types: List<UnwrappedType>): UnwrappedType {
    when (types.size) {
        0 -> error("Expected some types")
        1 -> return types.single()
    }
    var hasFlexibleTypes = false
    var hasErrorType = false
    val lowerBounds = types.map {
        hasErrorType = hasErrorType || it.isError
        when (it) {
            is SimpleType -> it
            is FlexibleType -> {
                if (it.isDynamic()) return it

                hasFlexibleTypes = true
                it.lowerBound
            }
        }
    }
    if (hasErrorType) {
        return ErrorUtils.createErrorType("Intersection of error types: $types")
    }

    if (!hasFlexibleTypes) {
        return TypeIntersector.intersectTypes(lowerBounds)
    }

    val upperBounds = types.map { it.upperIfFlexible() }
    /**
     * We should save this rules:
     *  - if for each type from types type is subtype of A, then intersectionType should be subtype of A
     *  - same for type B which is subtype of all types.
     *
     *  Note: when we construct intersection type of dynamic(or Raw type) & other type, we can get non-dynamic type.  // todo discuss
     */
    return KotlinTypeFactory.flexibleType(TypeIntersector.intersectTypes(lowerBounds), TypeIntersector.intersectTypes(upperBounds))
}


object TypeIntersector {

    internal fun intersectTypes(types: List<SimpleType>): SimpleType {
        assert(types.size > 1) {
            "Size should be at least 2, but it is ${types.size}"
        }
        val inputTypes = ArrayList<SimpleType>()
        for (type in types) {
            if (type.constructor is IntersectionTypeConstructor) {
                inputTypes.addAll(type.constructor.supertypes.map {
                    it.upperIfFlexible().let { if (type.isMarkedNullable) it.makeNullableAsSpecified(true) else it }
                })
            }
            else {
                inputTypes.add(type)
            }
        }
        val resultNullability = inputTypes.fold(ResultNullability.START, ResultNullability::combine)
        /**
         * resultNullability. Value description:
         * ACCEPT_NULL means that all types marked nullable
         * NOT_NULL means that there is one type which is subtype of Any => all types can be marked not nullable
         * UNKNOWN means, that we do not know, i.e. more precisely, all singleClassifier types marked nullable if any,
         * and other types is captured types or type parameters without not-null upper bound. Example: `String? & T` such types we should leave as is.
         */
        val correctNullability = inputTypes.mapTo(LinkedHashSet()) {
            if (resultNullability == ResultNullability.NOT_NULL) it.makeNullableAsSpecified(false) else it
        }

        return intersectTypesWithoutIntersectionType(correctNullability)
    }

    // nullability here is correct
    private fun intersectTypesWithoutIntersectionType(inputTypes: Set<SimpleType>): SimpleType {
        if (inputTypes.size == 1) return inputTypes.single()

        // Any and Nothing should leave
        // Note that duplicates should be dropped because we have Set here.
        val filteredSuperAndEqualTypes = ArrayList(inputTypes)
        val iterator = filteredSuperAndEqualTypes.iterator()
        while (iterator.hasNext()) {
            val upper = iterator.next()
            val strictSupertypeOrHasEqual = filteredSuperAndEqualTypes.any { lower ->
                lower !== upper && (isStrictSupertype(lower, upper) || NewKotlinTypeChecker.equalTypes(lower, upper))
            }

            if (strictSupertypeOrHasEqual) iterator.remove()
        }

        assert(filteredSuperAndEqualTypes.isNotEmpty()) {
            "This collections cannot be empty! input types: ${inputTypes.joinToString()}"
        }

        if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

        val constructor = IntersectionTypeConstructor(inputTypes)
        return KotlinTypeFactory.simpleType(Annotations.EMPTY, constructor, listOf(), false, constructor.createScopeForKotlinType())
    }

    private fun isStrictSupertype(subtype: KotlinType, supertype: KotlinType): Boolean {
        return with(NewKotlinTypeChecker) {
            isSubtypeOf(subtype, supertype) && !isSubtypeOf(supertype, subtype)
        }
    }

    /**
     * Let T is type parameter with upper bound Any?. resultNullability(String? & T) = UNKNOWN => String? & T
     */
    private enum class ResultNullability {
        START {
            override fun combine(nextType: UnwrappedType) = nextType.resultNullability
        },
        ACCEPT_NULL {
            override fun combine(nextType: UnwrappedType) = nextType.resultNullability
        },
        // example: type parameter without not-null supertype
        UNKNOWN {
            override fun combine(nextType: UnwrappedType) =
                    nextType.resultNullability.let {
                        if (it == ACCEPT_NULL) this else it
                    }
        },
        NOT_NULL {
            override fun combine(nextType: UnwrappedType) = this
        };

        abstract fun combine(nextType: UnwrappedType): ResultNullability

        protected val UnwrappedType.resultNullability: ResultNullability
            get() {
                if (isMarkedNullable) return ACCEPT_NULL

                if (NullabilityChecker.isSubtypeOfAny(this)) {
                    return NOT_NULL
                }
                else {
                    return UNKNOWN
                }
            }
    }
}