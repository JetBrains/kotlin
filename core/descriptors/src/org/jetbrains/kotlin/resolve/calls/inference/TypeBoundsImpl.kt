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
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import java.util.LinkedHashSet
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.types.singleBestRepresentative

public class TypeBoundsImpl(
        override val typeVariable: TypeParameterDescriptor,
        override val varianceOfPosition: Variance
) : TypeBounds {
    override val bounds = LinkedHashSet<Bound>()

    private var resultValues: Collection<JetType>? = null

    public fun addBound(kind: BoundKind, constrainingType: JetType, position: ConstraintPosition) {
        resultValues = null
        bounds.add(Bound(constrainingType, kind, position))
    }

    override fun isEmpty(): Boolean {
        return getValues().isEmpty()
    }

    private fun filterBounds(bounds: Collection<Bound>, kind: BoundKind): Set<JetType> {
        return filterBounds(bounds, kind, null)
    }

    private fun filterBounds(bounds: Collection<Bound>, kind: BoundKind, errorValues: MutableCollection<JetType>?): Set<JetType> {
        val result = LinkedHashSet<JetType>()
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

    fun copy(): TypeBoundsImpl {
        val typeBounds = TypeBoundsImpl(typeVariable, varianceOfPosition)
        typeBounds.bounds.addAll(bounds)
        typeBounds.resultValues = resultValues
        return typeBounds
    }

    public fun filter(condition: (ConstraintPosition) -> Boolean): TypeBoundsImpl {
        val result = TypeBoundsImpl(typeVariable, varianceOfPosition)
        result.bounds.addAll(bounds.filter { condition(it.position) })
        return result
    }

    override fun getValue(): JetType? {
        val values = getValues()
        if (values.size() == 1) {
            return values.iterator().next()
        }
        return null
    }

    override fun getValues(): Collection<JetType> {
        if (resultValues == null) {
            resultValues = computeValues()
        }
        return resultValues!!
    }

    private fun computeValues(): Collection<JetType> {
        val values = LinkedHashSet<JetType>()
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
            if (tryPossibleAnswer(bestFit)) {
                return listOf(bestFit)
            }
        }
        values.addAll(exactBounds)

        val (numberLowerBounds, generalLowerBounds) =
                filterBounds(bounds, LOWER_BOUND, values).partition { it.getConstructor() is IntegerValueTypeConstructor }

        val superTypeOfLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(generalLowerBounds)
        if (tryPossibleAnswer(superTypeOfLowerBounds)) {
            return setOf(superTypeOfLowerBounds!!)
        }
        values.addIfNotNull(superTypeOfLowerBounds)

        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        val superTypeOfNumberLowerBounds = TypeUtils.commonSupertypeForNumberTypes(numberLowerBounds)
        if (tryPossibleAnswer(superTypeOfNumberLowerBounds)) {
            return setOf(superTypeOfNumberLowerBounds!!)
        }
        values.addIfNotNull(superTypeOfNumberLowerBounds)

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            val superTypeOfAllLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(listOf(superTypeOfLowerBounds, superTypeOfNumberLowerBounds))
            if (tryPossibleAnswer(superTypeOfAllLowerBounds)) {
                return setOf(superTypeOfAllLowerBounds!!)
            }
        }

        val upperBounds = filterBounds(bounds, TypeBounds.BoundKind.UPPER_BOUND, values)
        val intersectionOfUpperBounds = TypeUtils.intersect(JetTypeChecker.DEFAULT, upperBounds)
        if (!upperBounds.isEmpty() && intersectionOfUpperBounds != null) {
            if (tryPossibleAnswer(intersectionOfUpperBounds)) {
                return setOf(intersectionOfUpperBounds)
            }
        }

        values.addAll(filterBounds(bounds, TypeBounds.BoundKind.UPPER_BOUND))

        return values
    }

    private fun tryPossibleAnswer(possibleAnswer: JetType?): Boolean {
        if (possibleAnswer == null) return false
        // a captured type might be an answer
        if (!possibleAnswer.getConstructor().isDenotable() && !possibleAnswer.isCaptured()) return false

        for (bound in bounds) {
            when (bound.kind) {
                LOWER_BOUND -> if (!JetTypeChecker.DEFAULT.isSubtypeOf(bound.constrainingType, possibleAnswer)) {
                    return false
                }

                UPPER_BOUND -> if (!JetTypeChecker.DEFAULT.isSubtypeOf(possibleAnswer, bound.constrainingType)) {
                    return false
                }

                EXACT_BOUND -> if (!JetTypeChecker.DEFAULT.equalTypes(bound.constrainingType, possibleAnswer)) {
                    return false
                }
            }
        }
        return true
    }
}
