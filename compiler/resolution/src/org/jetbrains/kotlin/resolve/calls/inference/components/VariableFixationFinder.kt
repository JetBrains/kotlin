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
import org.jetbrains.kotlin.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedKotlinCallArgument
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.contains
import java.util.*

class VariableFixationFinder {
    interface Context {
        val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints>
        val postponedArguments: List<PostponedKotlinCallArgument>
    }

    data class VariableForFixation(val variable: TypeConstructor, val hasProperConstraint: Boolean)

    fun findFirstVariableForFixation(
            c: Context,
            type: ConstraintSystemCompleter.CompletionType,
            topLevelType: UnwrappedType
    ): VariableForFixation? = c.findTypeVariableForFixation(type, topLevelType)

    private fun Context.findTypeVariableForFixation(
            type: ConstraintSystemCompleter.CompletionType,
            topLevelType: UnwrappedType
    ): VariableForFixation? {
        val edgesProvider = EdgesProvider(notFixedTypeVariables)

        val forbiddenForFixation = findAllForbiddenForFixation(edgesProvider, postponedArguments, type, topLevelType)
        val relatedToAllOutputTypes = findAllRelatedToAllOutputTypes(edgesProvider, postponedArguments)

        // first node not related to all output node (available for fixation) if there is one.
        // otherwise first node available for fixation
        val initialOrder = notFixedTypeVariables.keys.sortByInitialOrder()

        // not related to output types and allowed for fixations
        var nodeForFixation = initialOrder.firstOrNull { !forbiddenForFixation.contains(it) && !relatedToAllOutputTypes.contains(it) }

        if (nodeForFixation == null) {
            nodeForFixation = initialOrder.firstOrNull { !forbiddenForFixation.contains(it) }
        }

        if (nodeForFixation == null) return null

        val relatedNodes = edgesProvider.computeAllRelatedTypeVariables(nodeForFixation).sortByInitialOrder()
        // we can fix nodeForFixation, because it is the first node from all relatedNodes via initial order
        // but now we try to sort them respectfully to nodes like T <: Foo<S>, so S is going before T

        // noDependencyAndHasProperConstraints
        relatedNodes.firstOrNull { !hasDependencyToOtherTypeVariables(it) && variableHasProperConstraints(it) }?.let {
            return VariableForFixation(it, true)
        }

        // hasProperConstraint
        relatedNodes.firstOrNull { variableHasProperConstraints(it) }?.let { return VariableForFixation(it, true) }

        return VariableForFixation(relatedNodes.first(), false)
    }

    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructor): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            if (constraint.type.arguments.isNotEmpty() && constraint.type.contains { notFixedTypeVariables.containsKey(it.constructor) }) {
                return true
            }
        }
        return false
    }

    private fun Context.variableHasProperConstraints(variable: TypeConstructor): Boolean =
            notFixedTypeVariables[variable]?.constraints?.any { isProperArgumentConstraint(it) } ?: false

    private fun Context.isProperArgumentConstraint(c: Constraint) =
            isProperType(c.type) && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition

    private fun Context.isProperType(type: UnwrappedType): Boolean =
            !type.contains { notFixedTypeVariables.containsKey(it.constructor) }

    private fun Collection<TypeConstructor>.sortByInitialOrder(): List<TypeConstructor> =
            sortedBy { toString() } // todo

    private fun findAllRelatedToAllOutputTypes(
            edgesProvider: EdgesProvider,
            postponedArguments: Collection<PostponedKotlinCallArgument>
    ): Set<TypeConstructor> =
            postponedArguments.filterNot { it.analyzed }.flatMapTo(hashSetOf<TypeConstructor>()) {
                it.outputType?.let { edgesProvider.computeAllRelatedTypeVariables(it) } ?: emptyList()
            }


    private fun findAllForbiddenForFixation(
            edgesProvider: EdgesProvider,
            postponedArguments: Collection<PostponedKotlinCallArgument>,
            completionType: ConstraintSystemCompleter.CompletionType,
            topLevelType: UnwrappedType
    ): Set<TypeConstructor> {
        if (completionType == ConstraintSystemCompleter.CompletionType.FULL) return emptySet()

        val initialNodes = edgesProvider.computeAllRelatedTypeVariables(topLevelType)
        if (initialNodes.isEmpty()) return emptySet()

        val result = HashSet(initialNodes) // this set always is closed set by related edges
        val notProcessedNodes = Stack<TypeConstructor>().apply { addAll(initialNodes) }

        while (notProcessedNodes.isNotEmpty()) {
            val startNode = notProcessedNodes.pop()

            for (postponeArgument in postponedArguments) {
                val resultType = postponeArgument.outputType ?: continue
                val resultIsDependedFromStartNode = postponeArgument.inputTypes.any { it.contains { it.constructor == startNode } }

                if (resultIsDependedFromStartNode) {
                    edgesProvider.forAllMyNodes(resultType) { newNode ->
                        if (!result.contains(newNode)) {
                            val newRelatedNodes = edgesProvider.computeAllRelatedTypeVariables(startNode)
                            notProcessedNodes.addAll(newRelatedNodes)
                            result.addAll(newRelatedNodes)
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * Direct edges it is edge from type variable to type variables inside constraints on current type variable.
     * Such edges can be calculated explicitly only via VariablesWithConstraints.
     * Reversed edges should be cached, because it isn't trivial to calculate them. Such edges called "indirect edges"
     *
     * In future we can do not recreate this after type variable fixation, because edges cannot be deleted ->
     * we should add new edges and filter fixed type variables
     */
    private class EdgesProvider(
            val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints>
    ) {
        private val indirectEdges: MutableMap<TypeConstructor, MutableSet<TypeConstructor>> = hashMapOf()
        private val directEdges: MutableMap<TypeConstructor, MutableSet<TypeConstructor>> = hashMapOf()

        init {
            fun addDirectEdge(from: TypeConstructor, to: TypeConstructor) {
                directEdges.getOrPut(from) { hashSetOf() }.add(to)
                indirectEdges.getOrPut(to) { hashSetOf() }.add(from)
            }

            for (variableWithConstraints in notFixedTypeVariables.values) {
                val from = variableWithConstraints.typeVariable.freshTypeConstructor

                for (constraint in variableWithConstraints.constraints) {
                    constraint.type.contains {
                        if (notFixedTypeVariables.containsKey(it.constructor)) {
                            addDirectEdge(from, it.constructor)
                        }

                        false
                    }
                }
            }
        }

        private fun getDirectEdges(from: TypeConstructor): Set<TypeConstructor> = directEdges[from] ?: emptySet()
        private fun getIndirectEdges(from: TypeConstructor): Set<TypeConstructor> = indirectEdges[from] ?: emptySet()

        private fun getAllEdges(from: TypeConstructor): Set<TypeConstructor> = getDirectEdges(from) + getIndirectEdges(from)

        fun computeAllRelatedTypeVariables(startType: UnwrappedType): Set<TypeConstructor> =
                HashSet<TypeConstructor>().apply {
                    forAllMyNodes(startType) { addAllRelatedNodes(this, it) }
                }

        fun computeAllRelatedTypeVariables(startNode: TypeConstructor): Set<TypeConstructor> = HashSet<TypeConstructor>().apply {
            if (isMyTypeVariable(startNode)) addAllRelatedNodes(this, startNode)
        }

        private fun isMyTypeVariable(typeConstructor: TypeConstructor) = notFixedTypeVariables.containsKey(typeConstructor)

        private fun addAllRelatedNodes(to: MutableSet<TypeConstructor>, node: TypeConstructor) {
            if (to.add(node)) {
                for (relatedNode in getAllEdges(node)) {
                    addAllRelatedNodes(to, relatedNode)
                }
            }
        }

        fun forAllMyNodes(startType: UnwrappedType, action: (TypeConstructor) -> Unit) = startType.contains {
            if (isMyTypeVariable(it.constructor)) action(it.constructor)

            false
        }
    }


}