/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

public class TypeBoundsImpl(override val typeVariable: TypeParameterDescriptor) : TypeBounds {
    override val bounds = ArrayList<Bound>()

    private var resultValues: Collection<KotlinType>? = null

    var isFixed: Boolean = false
        private set

    public fun setFixed() {
        isFixed = true
    }

    public fun addBound(bound: Bound) {
        resultValues = null
        assert(bound.typeVariable == typeVariable) {
            "$bound is added for incorrect type variable ${bound.typeVariable.name}. Expected: ${typeVariable.name}"
        }
        bounds.add(bound)
    }

    private fun filterBounds(bounds: Collection<Bound>, kind: BoundKind, errorValues: MutableCollection<KotlinType>? = null): Set<KotlinType> {
        val result = LinkedHashSet<KotlinType>()
        for (bound in bounds) {
            if (bound.kind == kind) {
                if (!ErrorUtils.containsErrorType(bound.constrainingType)) {
                    result.add(bound.constrainingType)
                }
                else {
                    errorValues?.add(bound.constrainingType)
                }
            }
        }
        return result
    }

    public fun filter(condition: (ConstraintPosition) -> Boolean): TypeBoundsImpl {
        val result = TypeBoundsImpl(typeVariable)
        result.bounds.addAll(bounds.filter { condition(it.position) })
        return result
    }

    override val values: Collection<KotlinType>
        get() {
            if (resultValues == null) {
                resultValues = computeValues()
            }
            return resultValues!!
        }

    private fun computeValues(): Collection<KotlinType> {
        val values = LinkedHashSet<KotlinType>()
        val bounds = bounds.filter { it.isProper }

        if (bounds.isEmpty()) {
            return listOf()
        }
        val hasStrongBound = bounds.any { it.position.isStrong() }
        if (!hasStrongBound) {
            return listOf()
        }

        val exactBounds = filterBounds(bounds, EXACT_BOUND, values)
        val bestFit = exactBounds.singleBestRepresentative()
        if (bestFit != null) {
            if (tryPossibleAnswer(bounds, bestFit)) {
                return listOf(bestFit)
            }
        }
        values.addAll(exactBounds)

        val (numberLowerBounds, generalLowerBounds) =
                filterBounds(bounds, LOWER_BOUND, values).partition { it.constructor is IntegerValueTypeConstructor }

        val superTypeOfLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(generalLowerBounds)
        if (tryPossibleAnswer(bounds, superTypeOfLowerBounds)) {
            return setOf(superTypeOfLowerBounds!!)
        }
        values.addIfNotNull(superTypeOfLowerBounds)

        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        val superTypeOfNumberLowerBounds = commonSupertypeForNumberTypes(numberLowerBounds)
        if (tryPossibleAnswer(bounds, superTypeOfNumberLowerBounds)) {
            return setOf(superTypeOfNumberLowerBounds!!)
        }
        values.addIfNotNull(superTypeOfNumberLowerBounds)

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            val superTypeOfAllLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(listOf(superTypeOfLowerBounds, superTypeOfNumberLowerBounds))
            if (tryPossibleAnswer(bounds, superTypeOfAllLowerBounds)) {
                return setOf(superTypeOfAllLowerBounds!!)
            }
        }

        val upperBounds = filterBounds(bounds, TypeBounds.BoundKind.UPPER_BOUND, values)
        if (upperBounds.isNotEmpty()) {
            val intersectionOfUpperBounds = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, upperBounds)
            if (intersectionOfUpperBounds != null && tryPossibleAnswer(bounds, intersectionOfUpperBounds)) {
                return setOf(intersectionOfUpperBounds)
            }
        }

        values.addAll(filterBounds(bounds, TypeBounds.BoundKind.UPPER_BOUND))

        if (values.size == 1 && typeVariable.hasOnlyInputTypesAnnotation() && !tryPossibleAnswer(bounds, values.first())) return listOf()

        return values
    }

    private fun checkOnlyInputTypes(bounds: Collection<Bound>, possibleAnswer: KotlinType): Boolean {
        if (!typeVariable.hasOnlyInputTypesAnnotation()) return true

        // Only type mentioned in bounds might be the result
        val typesInBoundsSet = bounds.filter { it.isProper && it.constrainingType.constructor.isDenotable }.map { it.constrainingType }.toSet()
        // Flexible types are equal to inflexible
        if (typesInBoundsSet.any { KotlinTypeChecker.DEFAULT.equalTypes(it, possibleAnswer) }) return true

        // For non-denotable number types only, no valid types are mentioned, so common supertype is valid
        val numberLowerBounds = filterBounds(bounds, LOWER_BOUND).filter { it.constructor is IntegerValueTypeConstructor }
        val superTypeOfNumberLowerBounds = commonSupertypeForNumberTypes(numberLowerBounds)
        if (possibleAnswer == superTypeOfNumberLowerBounds) return true

        return false
    }

    private fun tryPossibleAnswer(bounds: Collection<Bound>, possibleAnswer: KotlinType?): Boolean {
        if (possibleAnswer == null) return false
        // a captured type might be an answer
        if (!possibleAnswer.constructor.isDenotable && !possibleAnswer.isCaptured()) return false

        if (!checkOnlyInputTypes(bounds, possibleAnswer)) return false

        for (bound in bounds) {
            when (bound.kind) {
                LOWER_BOUND -> if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(bound.constrainingType, possibleAnswer)) {
                    return false
                }

                UPPER_BOUND -> if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(possibleAnswer, bound.constrainingType)) {
                    return false
                }

                EXACT_BOUND -> if (!KotlinTypeChecker.DEFAULT.equalTypes(bound.constrainingType, possibleAnswer)) {
                    return false
                }
            }
        }
        return true
    }

    private fun commonSupertypeForNumberTypes(numberLowerBounds: Collection<KotlinType>): KotlinType? {
        if (numberLowerBounds.isEmpty()) return null
        val intersectionOfSupertypes = getIntersectionOfSupertypes(numberLowerBounds)
        return TypeUtils.getDefaultPrimitiveNumberType(intersectionOfSupertypes) ?:
                CommonSupertypes.commonSupertype(numberLowerBounds)
    }

    private fun getIntersectionOfSupertypes(types: Collection<KotlinType>): Set<KotlinType> {
        val upperBounds = HashSet<KotlinType>()
        for (type in types) {
            val supertypes = type.constructor.supertypes
            if (upperBounds.isEmpty()) {
                upperBounds.addAll(supertypes)
            }
            else {
                upperBounds.retainAll(supertypes)
            }
        }
        return upperBounds
    }
}
