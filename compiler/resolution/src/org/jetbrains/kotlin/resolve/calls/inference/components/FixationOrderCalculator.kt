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

import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedLambdaArgument
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.checker.isIntersectionType
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.collections.HashMap

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

        private val lambdaResultDependencyEdges = HashMap<Variable, MutableList<Variable>>()

        // first in the list -- first fix
        fun getCompletionOrder(topReturnType: UnwrappedType): List<NodeWithDirection> {
            setupDirections(topReturnType)

            computeLambdaResultDependencyEdges()

            return topologicalOrderWith0Priority().map { NodeWithDirection(it, directions[it] ?: ResolveDirection.UNKNOWN) }
        }

        private fun computeLambdaResultDependencyEdges() {
            val resolvedLambdaArguments = c.lambdaArguments.associateBy({ it.argument }, { it })

            for (variable in c.notFixedTypeVariables.values) {
                val lambdaResultVariable = variable.typeVariable.takeLambdaResultVariable() ?: continue

                val lambdaArgument = lambdaResultVariable.lambdaArgument

                if (resolvedLambdaArguments[lambdaArgument]?.analyzed ?: false) continue

                for (constraint in variable.constraints) {
                    val initialDirection = when (constraint.kind) {
                        ConstraintKind.LOWER -> ResolveDirection.TO_SUBTYPE
                        ConstraintKind.UPPER -> ResolveDirection.TO_SUPERTYPE
                        ConstraintKind.EQUALITY -> ResolveDirection.UNKNOWN // ???
                    }

                    constraint.type.visitType(initialDirection) { constituentVariable, direction ->
                        val constituentVariableDirection = directions.getOrElse(constituentVariable) { ResolveDirection.UNKNOWN }

                        val constituentTypeVariable = constituentVariable.typeVariable
                        if (constituentTypeVariable is LambdaTypeVariable) {
                            if (constituentTypeVariable.lambdaArgument == lambdaArgument) return@visitType
                        }

                        if (direction == ResolveDirection.UNKNOWN || direction == constituentVariableDirection) {
                            lambdaResultDependencyEdges.getOrPut(constituentVariable) { SmartList() }.add(variable)
                        }
                    }
                }
            }
        }

        private fun topologicalOrderWith0Priority(): List<Variable> {
            val handler = object : DFS.CollectingNodeHandler<Variable, Variable, LinkedHashSet<Variable>>(LinkedHashSet()) {
                override fun afterChildren(current: Variable) {
                    // LAMBDA dependency edges should always be satisfied
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
                inner@ for (typeVariable in resolvedLambdaArgument.myTypeVariables) {
                    if (typeVariable.kind == LambdaTypeVariable.Kind.RETURN_TYPE) continue@inner

                    c.notFixedTypeVariables[typeVariable.freshTypeConstructor]?.let {
                        enterToNode(it, ResolveDirection.TO_SUBTYPE)
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
                        set.addAll(getLambdaResultDependencies(variable))
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

        private fun getLambdaResultDependencies(variable: Variable): List<Variable> =
                lambdaResultDependencyEdges.getOrElse(variable) { emptyList() }

        private fun getLambdaDependencies(variable: Variable): List<Variable> {
            val typeVariable = variable.typeVariable.takeLambdaResultVariable() ?: return emptyList()

            val resolvedLambdaArgument = c.lambdaArguments.find { it.argument == typeVariable.lambdaArgument } ?:
                                         error("Missing resolved lambda argument for ${typeVariable.lambdaArgument}")

            return buildVariablesList {
                for (lambdaTypeVariable in resolvedLambdaArgument.myTypeVariables) {
                    if (lambdaTypeVariable.kind == LambdaTypeVariable.Kind.RETURN_TYPE) continue
                    addIfNotNull(c.notFixedTypeVariables[lambdaTypeVariable.freshTypeConstructor])
                }
            }
        }

        private inline fun buildVariablesList(builder: MutableList<Variable>.() -> Unit): List<Variable> =
                SmartList<Variable>().apply(builder).toList()

        private fun NewTypeVariable.takeLambdaResultVariable(): LambdaTypeVariable? =
                if (this is LambdaTypeVariable && this.kind == LambdaTypeVariable.Kind.RETURN_TYPE) this else null

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