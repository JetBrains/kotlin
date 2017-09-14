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

import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtom
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.checker.isIntersectionType
import org.jetbrains.kotlin.utils.SmartList


private typealias Variable = VariableWithConstraints

class TypeVariableDirectionCalculator(
        private val c: VariableFixationFinder.Context,
        private val postponedKtPrimitives: List<PostponedResolvedAtom>,
        topLevelType: UnwrappedType
) {
    enum class ResolveDirection {
        TO_SUBTYPE,
        TO_SUPERTYPE,
        UNKNOWN
    }

    data class NodeWithDirection(val variableWithConstraints: VariableWithConstraints, val direction: ResolveDirection) {
        override fun toString() = "$variableWithConstraints to $direction"
    }
    
    private val directions = HashMap<Variable, ResolveDirection>()

    init {
        setupDirections(topLevelType)
    }

    fun getDirection(typeVariable: Variable): ResolveDirection =
            directions.getOrDefault(typeVariable, ResolveDirection.UNKNOWN)

    private fun setupDirections(topReturnType: UnwrappedType) {
        topReturnType.visitType(ResolveDirection.TO_SUBTYPE) { variableWithConstraints, direction ->
            enterToNode(variableWithConstraints, direction)
        }
        for (postponedArgument in postponedKtPrimitives) {
            for (inputType in postponedArgument.inputTypes) {
                inputType.visitType(ResolveDirection.TO_SUBTYPE) { variableWithConstraints, direction ->
                    enterToNode(variableWithConstraints, direction)
                }
            }
        }
    }

    private fun enterToNode(variable: Variable, direction: ResolveDirection) {
        if (direction == ResolveDirection.UNKNOWN) return

        val previous = directions[variable]
        if (previous != null) {
            if (previous != direction) {
                directions[variable] = ResolveDirection.UNKNOWN
            }
            return
        }

        directions[variable] = direction

        for ((otherVariable, otherDirection) in getConstraintDependencies(variable, direction)) {
            enterToNode(otherVariable, otherDirection)
        }
    }

    private fun getConstraintDependencies(
            variable: Variable,
            direction: ResolveDirection
    ): List<NodeWithDirection> =
            SmartList<NodeWithDirection>().also { result ->
                for (constraint in variable.constraints) {
                    if (!isInterestingConstraint(direction, constraint)) continue

                    constraint.type.visitType(direction) { nodeVariable, nodeDirection ->
                        result.add(NodeWithDirection(nodeVariable, nodeDirection))
                    }
                }
            }


    private fun isInterestingConstraint(direction: ResolveDirection, constraint: Constraint): Boolean =
            !(direction == ResolveDirection.TO_SUBTYPE && constraint.kind == ConstraintKind.UPPER) &&
            !(direction == ResolveDirection.TO_SUPERTYPE && constraint.kind == ConstraintKind.LOWER)

    private fun UnwrappedType.visitType(startDirection: ResolveDirection, action: (variable: Variable, direction: ResolveDirection) -> Unit) =
            when (this) {
                is SimpleType -> visitType(startDirection, action)
                is FlexibleType -> {
                    lowerBound.visitType(startDirection, action)
                    upperBound.visitType(startDirection, action)
                }
            }

    private fun SimpleType.visitType(startDirection: ResolveDirection, action: (variable: Variable, direction: ResolveDirection) -> Unit) {
        if (isIntersectionType) {
            constructor.supertypes.forEach {
                it.unwrap().visitType(startDirection, action)
            }
            return
        }

        if (arguments.isEmpty()) {
            c.notFixedTypeVariables[constructor]?.let {
                action(it, startDirection)
            }
            return
        }

        val parameters = constructor.parameters
        if (parameters.size != arguments.size) return // incorrect type

        for ((argument, parameter) in arguments.zip(parameters)) {
            if (argument.isStarProjection) continue

            val variance = NewKotlinTypeChecker.effectiveVariance(parameter.variance, argument.projectionKind) ?: Variance.INVARIANT
            val innerDirection = when (variance) {
                Variance.INVARIANT -> ResolveDirection.UNKNOWN
                Variance.OUT_VARIANCE -> startDirection
                Variance.IN_VARIANCE -> startDirection.opposite()
            }

            argument.type.unwrap().visitType(innerDirection, action)
        }
    }

    private fun ResolveDirection.opposite() = when (this) {
        ResolveDirection.UNKNOWN -> ResolveDirection.UNKNOWN
        ResolveDirection.TO_SUPERTYPE -> ResolveDirection.TO_SUBTYPE
        ResolveDirection.TO_SUBTYPE -> ResolveDirection.TO_SUPERTYPE
    }
}