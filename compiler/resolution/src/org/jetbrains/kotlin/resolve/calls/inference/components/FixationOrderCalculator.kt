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

import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.ResolvedLambdaArgument
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.checker.isIntersectionType
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartList

private typealias Variable = VariableWithConstraints

class FixationOrderCalculator {
    enum class ResolveDirection {
        TO_SUBTYPE,
        TO_SUPERTYPE,
        UNKNOWN
    }

    data class NodeWithDirection(val variableWithConstraints: VariableWithConstraints, val direction: ResolveDirection) {
        override fun toString() = "$variableWithConstraints to $direction"
    }

    interface Context {
        val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints>
        val lambdaArguments: List<ResolvedLambdaArgument>
    }

    fun computeCompletionOrder(
            c: Context,
            topReturnType: UnwrappedType
    ): List<NodeWithDirection> = DependencyGraph(c).getCompletionOrder(topReturnType)

    /**
     * U depends-on V if one of the following conditions is met:
     *
     *      LAMBDA
     *              result type U depends-on all parameters types V of the corresponding lambda
     *
     *      LAMBDA-RESULT
     *              Since there is no separate type variables for lambda such edges removed for now
     *
     *              V is a lambda result type variable,
     *              V <: T constraint exists for V,
     *              U is a constituent type of T in position matching approximation direction for U
     *
     *      STRONG-CONSTRAINT
     *              'U <op> T' constraint exists for U,
     *              <op> is a constraint operator relevant to U approximation direction,
     *              V is a proper constituent type of T
     *
     *      WEAK-CONSTRAINT
     *              'U <op> V' constraint exists for U,
     *              <op> is a constraint operator relevant to U approximation direction
     */
    private class DependencyGraph(val c: Context) {
        private val directions = HashMap<Variable, ResolveDirection>()

        private val lambdaEdges = HashMap<Variable, MutableSet<Variable>>()

        // first in the list -- first fix
        fun getCompletionOrder(topReturnType: UnwrappedType): List<NodeWithDirection> {
            setupDirections(topReturnType)

            buildLambdaEdges()

            return topologicalOrderWith0Priority().map { NodeWithDirection(it, directions[it] ?: ResolveDirection.UNKNOWN) }
        }

        private fun buildLambdaEdges() {
            for (lambdaArgument in c.lambdaArguments) {
                if (lambdaArgument.analyzed) continue

                val typeVariablesInReturnType = SmartList<Variable>()
                lambdaArgument.returnType.findTypeVariables(typeVariablesInReturnType)

                if (typeVariablesInReturnType.isEmpty()) continue // optimization

                val typeVariablesInParameters = SmartList<Variable>()
                lambdaArgument.inputTypes.forEach { it.findTypeVariables(typeVariablesInParameters) }

                for (returnTypeVariable in typeVariablesInReturnType) {
                    lambdaEdges.getOrPut(returnTypeVariable) { LinkedHashSet() }.addAll(typeVariablesInParameters)
                }
            }
        }

        private fun UnwrappedType.findTypeVariables(to: MutableCollection<Variable>) = contains {
            c.notFixedTypeVariables[it.constructor]?.let { variable -> to.add(variable) }
            false
        }

        private fun topologicalOrderWith0Priority(): List<Variable> {
            val handler = object : DFS.CollectingNodeHandler<Variable, Variable, LinkedHashSet<Variable>>(LinkedHashSet()) {
                override fun afterChildren(current: Variable) {
                    // LAMBDA dependency edges should always be satisfied
                    // Note that cyclic by lambda edges are possible
                    result.addAll(getLambdaDependencies(current))

                    result.add(current)
                }
            }

            for (typeVariable in c.notFixedTypeVariables.values.sortByTypeVariable()) {
                DFS.doDfs(typeVariable, DFS.Neighbors(this::getEdges), DFS.VisitedWithSet<Variable>(), handler)
            }
            return handler.result().toList()
        }


        private fun setupDirections(topReturnType: UnwrappedType) {
            topReturnType.visitType(ResolveDirection.TO_SUBTYPE) { variableWithConstraints, direction ->
                enterToNode(variableWithConstraints, direction)
            }
            for (resolvedLambdaArgument in c.lambdaArguments) {
                inner@ for (inputType in resolvedLambdaArgument.inputTypes) {
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

        private fun getEdges(variable: Variable): List<Variable> {
            val direction = directions[variable] ?: ResolveDirection.UNKNOWN
            val constraintEdges =
                    LinkedHashSet<Variable>().also { set ->
                        getConstraintDependencies(variable, direction).mapTo(set) { it.variableWithConstraints }
                    }.toList().sortByTypeVariable()
            val lambdaEdges = getLambdaDependencies(variable).sortByTypeVariable()
            return constraintEdges + lambdaEdges
        }

        private fun Collection<Variable>.sortByTypeVariable() =
                // TODO hack, provide some reasonable stable order
                sortedBy { it.typeVariable.toString() }

        private enum class ConstraintDependencyKind { STRONG, WEAK }

        private fun getConstraintDependencies(
                variableWithConstraints: Variable,
                direction: ResolveDirection,
                filterByDependencyKind: ConstraintDependencyKind? = null
        ): List<NodeWithDirection> =
                SmartList<NodeWithDirection>().also { result ->
                    for (constraint in variableWithConstraints.constraints) {
                        if (!isInterestingConstraint(direction, constraint)) continue

                        if (filterByDependencyKind == null || filterByDependencyKind == getConstraintDependencyKind(constraint)) {
                            constraint.type.visitType(direction) { nodeVariable, nodeDirection ->
                                result.add(NodeWithDirection(nodeVariable, nodeDirection))
                            }
                        }
                    }
                }

        private fun getConstraintDependencyKind(constraint: Constraint): ConstraintDependencyKind =
                if (c.notFixedTypeVariables.containsKey(constraint.type.constructor))
                    ConstraintDependencyKind.WEAK
                else
                    ConstraintDependencyKind.STRONG

        private fun isInterestingConstraint(direction: ResolveDirection, constraint: Constraint): Boolean =
                !(direction == ResolveDirection.TO_SUBTYPE && constraint.kind == ConstraintKind.UPPER) &&
                !(direction == ResolveDirection.TO_SUPERTYPE && constraint.kind == ConstraintKind.LOWER)


        private fun getLambdaDependencies(variable: Variable): List<Variable> = lambdaEdges[variable]?.toList() ?: emptyList()

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

}