/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.FixationOrderCalculator.ResolveDirection
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.intersectTypes
import java.util.*

class ResultTypeResolver(
        val typeApproximator: TypeApproximator
) {
    interface Context {
        fun isProperType(type: UnwrappedType): Boolean
    }

    fun findResultType(c: Context, variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): UnwrappedType? {
        findResultIfThereIsEqualsConstraint(c, variableWithConstraints, allowedFixToNotProperType = false)?.let { return it }

        val builtIns = variableWithConstraints.typeVariable.freshTypeConstructor.builtIns

        if (direction == ResolveDirection.TO_SUBTYPE || direction == ResolveDirection.UNKNOWN) {
            val lowerConstraints = variableWithConstraints.constraints.filter { it.kind == ConstraintKind.LOWER && c.isProperType(it.type) }
            if (lowerConstraints.isNotEmpty()) {
                val commonSupertype = NewCommonSuperTypeCalculator.commonSuperType(convertLowerTypesWithKnowledgeOfNumberTypes(lowerConstraints))
                /**
                 *
                 * fun <T> Array<out T>.intersect(other: Iterable<T>) {
                 *      val set = toMutableSet()
                 *      set.retainAll(other)
                 * }
                 * fun <X> Array<out X>.toMutableSet(): MutableSet<X> = ...
                 * fun <Y> MutableCollection<in Y>.retainAll(elements: Iterable<Y>) {}
                 *
                 * Here, when we solve type system for `toMutableSet` we have the following constrains:
                 * Array<C(out T)> <: Array<out X> => C(out X) <: T.
                 * If we fix it to T = C(out X) then return type of `toMutableSet()` will be `MutableSet<C(out X)>`
                 * and type of variable `set` will be `MutableSet<out T>` and the following line will have contradiction.
                 *
                 * To fix this problem when we fix variable, we will approximate captured types before fixation.
                 *
                 */

                return typeApproximator.approximateToSuperType(commonSupertype, TypeApproximatorConfiguration.CapturedTypesApproximation) ?: commonSupertype
            }
        }

        // direction == TO_SUPER or there is no LOWER bounds
        val upperConstraints = variableWithConstraints.constraints.filter { it.kind == ConstraintKind.UPPER && c.isProperType(it.type) }
        if (upperConstraints.isNotEmpty()) {
            val upperType = intersectTypes(upperConstraints.map { it.type })

            return typeApproximator.approximateToSubType(upperType, TypeApproximatorConfiguration.CapturedTypesApproximation) ?: upperType
        }

        // no proper constraints
        if (direction == ResolveDirection.TO_SUBTYPE) {
            return builtIns.nothingType
        }
        else {
            return builtIns.anyType
        }
    }

    fun findResultIfThereIsEqualsConstraint(
            c: Context,
            variableWithConstraints: VariableWithConstraints,
            allowedFixToNotProperType: Boolean = false
    ): UnwrappedType? {
        val properEqualsConstraint = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && c.isProperType(it.type)
        }

        if (properEqualsConstraint.isNotEmpty()) {
            return properEqualsConstraint.map { it.type }.singleBestRepresentative()?.unwrap()
                   ?: properEqualsConstraint.first().type // seems like constraint system has contradiction
        }
        if (!allowedFixToNotProperType) return null

        val notProperEqualsConstraint = variableWithConstraints.constraints.filter { it.kind == ConstraintKind.EQUALITY }

        // may be we should just firstOrNull
        return notProperEqualsConstraint.singleOrNull()?.type
    }


    private fun convertLowerTypesWithKnowledgeOfNumberTypes(lowerConstraints: Collection<Constraint>): List<UnwrappedType> {
        if (lowerConstraints.isEmpty()) return emptyList()

        val (numberLowerBounds, generalLowerBounds) = lowerConstraints.map { it.type }.partition { it.isNumberValueType() }

        val numberType = commonSupertypeForNumberTypes(numberLowerBounds) ?: return generalLowerBounds
        return generalLowerBounds + numberType
    }

    private fun KotlinType.isNumberValueType() =
            constructor is IntegerValueTypeConstructor ||
            (constructor is IntersectionTypeConstructor && constructor.supertypes.all { it.isPrimitiveIntegerType() } )

    private fun KotlinType.isPrimitiveIntegerType() =
            KotlinBuiltIns.isByte(this) ||
            KotlinBuiltIns.isShort(this) ||
            KotlinBuiltIns.isInt(this) ||
            KotlinBuiltIns.isLong(this)

    private fun commonSupertypeForNumberTypes(numberLowerBounds: List<UnwrappedType>): UnwrappedType? {
        if (numberLowerBounds.isEmpty()) return null
        val intersectionOfSupertypes = getIntersectionOfSupertypes(numberLowerBounds)
        return TypeUtils.getDefaultPrimitiveNumberType(intersectionOfSupertypes)?.unwrap() ?:
               NewCommonSuperTypeCalculator.commonSuperType(numberLowerBounds)
    }

    private fun getIntersectionOfSupertypes(types: Collection<UnwrappedType>): Set<UnwrappedType> {
        val upperBounds = HashSet<UnwrappedType>()
        for (type in types) {
            val supertypes = type.constructor.supertypes.map { it.unwrap() }
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