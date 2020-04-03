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

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator.ResolveDirection
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*

class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
) {
    interface Context : TypeSystemInferenceExtensionContext {
        fun isProperType(type: KotlinTypeMarker): Boolean
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
        fun isReified(variable: TypeVariableMarker): Boolean
    }

    fun findResultType(c: Context, variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): KotlinTypeMarker {
        findResultTypeOrNull(c, variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return run {
            if (direction == ResolveDirection.TO_SUBTYPE) c.nothingType() else c.nullableAnyType()
        }
    }

    private fun findResultTypeOrNull(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: ResolveDirection
    ): KotlinTypeMarker? {
        findResultIfThereIsEqualsConstraint(c, variableWithConstraints)?.let { return it }

        val subType = c.findSubType(variableWithConstraints)
        val superType = c.findSuperType(variableWithConstraints)
        return if (direction == ResolveDirection.TO_SUBTYPE || direction == ResolveDirection.UNKNOWN) {
            c.resultType(subType, superType, variableWithConstraints)
        } else {
            c.resultType(superType, subType, variableWithConstraints)
        }
    }

    private fun Context.resultType(
        firstCandidate: KotlinTypeMarker?,
        secondCandidate: KotlinTypeMarker?,
        variableWithConstraints: VariableWithConstraints
    ): KotlinTypeMarker? {
        if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        if (isSuitableType(firstCandidate, variableWithConstraints)) return firstCandidate

        return if (isSuitableType(secondCandidate, variableWithConstraints)) {
            secondCandidate
        } else {
            firstCandidate
        }
    }

    private fun Context.specialResultForIntersectionType(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker,
    ): KotlinTypeMarker? {
        if (firstCandidate.typeConstructor() is IntersectionTypeConstructor) {
            if (!AbstractTypeChecker.isSubtypeOf(this, firstCandidate.toPublicType(), secondCandidate.toPublicType())) {
                return createTypeWithAlternativeForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    private fun KotlinTypeMarker.toPublicType(): KotlinTypeMarker =
        typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.PublicDeclaration) ?: this

    private fun Context.isSuitableType(resultType: KotlinTypeMarker, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { isProperType(it.type) }
        for (constraint in filteredConstraints) {
            if (!checkConstraint(this, constraint.type, constraint.kind, resultType)) return false
        }
        if (!trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) {
            if (resultType.isNullableType() && checkSingleLowerNullabilityConstraint(filteredConstraints)) return false
            if (isReified(variableWithConstraints.typeVariable)) return false
        }

        return true
    }

    private fun checkSingleLowerNullabilityConstraint(constraints: List<Constraint>): Boolean {
        return constraints.singleOrNull { it.kind.isLower() }?.isNullabilityConstraint ?: false
    }

    private fun Context.findSubType(variableWithConstraints: VariableWithConstraints): KotlinTypeMarker? {
        val lowerConstraintTypes = prepareLowerConstraints(variableWithConstraints.constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            var commonSuperType = computeCommonSuperType(types)

            if (commonSuperType.contains { it is StubTypeMarker }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it is StubTypeMarker }
                }

                if (typesWithoutStubs.isNotEmpty()) {
                    commonSuperType = computeCommonSuperType(typesWithoutStubs)
                } else {
                    return null
                }
            }

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

            return typeApproximator.approximateToSuperType(
                commonSuperType,
                TypeApproximatorConfiguration.InternalTypesApproximation
            ) ?: commonSuperType
        }

        return null
    }

    private fun Context.computeCommonSuperType(types: List<KotlinTypeMarker>): KotlinTypeMarker =
        with(NewCommonSuperTypeCalculator) { commonSuperType(types) }

    private fun Context.prepareLowerConstraints(constraints: List<Constraint>): List<KotlinTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<KotlinTypeMarker>()

        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue

            val type = constraint.type
            lowerConstraintTypes.add(type)

            if (isProperType(type)) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        if (!atLeastOneProper) return emptyList()
        if (!atLeastOneNonProper) return lowerConstraintTypes

        val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map { if (isProperType(it)) it else notFixedToStubTypesSubstitutor.safeSubstitute(it) }
    }

    private fun Context.sinkIntegerLiteralTypes(types: List<KotlinTypeMarker>): List<KotlinTypeMarker> {
        return types.sortedBy { type ->

            val containsILT = type.contains { it.asSimpleType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    private fun Context.findSuperType(variableWithConstraints: VariableWithConstraints): KotlinTypeMarker? {
        val upperConstraints =
            variableWithConstraints.constraints.filter { it.kind == ConstraintKind.UPPER && this@findSuperType.isProperType(it.type) }
        if (upperConstraints.isNotEmpty()) {
            val upperType = intersectTypes(upperConstraints.map { it.type })

            return typeApproximator.approximateToSubType(
                upperType,
                TypeApproximatorConfiguration.InternalTypesApproximation
            ) ?: upperType
        }
        return null
    }

    private fun findResultIfThereIsEqualsConstraint(c: Context, variableWithConstraints: VariableWithConstraints): KotlinTypeMarker? =
        with(c) {
            val properEqualityConstraints = variableWithConstraints.constraints.filter {
                it.kind == ConstraintKind.EQUALITY && c.isProperType(it.type)
            }

            return c.representativeFromEqualityConstraints(properEqualityConstraints)
        }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    private fun Context.representativeFromEqualityConstraints(constraints: List<Constraint>): KotlinTypeMarker? {
        if (constraints.isEmpty()) return null

        val constraintTypes = constraints.map { it.type }
        val nonLiteralTypes = constraintTypes.filter { !it.typeConstructor().isIntegerLiteralTypeConstructor() }
        return nonLiteralTypes.singleBestRepresentative()
            ?: constraintTypes.singleBestRepresentative()
            ?: constraintTypes.first() // seems like constraint system has contradiction
    }
}