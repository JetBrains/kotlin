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

import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtom
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.utils.SmartSet

class TypeVariableDependencyInformationProvider(
    private val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>,
    private val postponedKtPrimitives: List<PostponedResolvedAtom>,
    private val topLevelType: KotlinTypeMarker?,
    private val typeSystemContext: TypeSystemInferenceExtensionContext
) {
    // not oriented edges
    private val constrainEdges: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    // oriented edges
    private val postponeArgumentsEdges: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    private val relatedToAllOutputTypes: MutableSet<TypeConstructorMarker> = hashSetOf()
    private val relatedToTopLevelType: MutableSet<TypeConstructorMarker> = hashSetOf()

    init {
        computeConstraintEdges()
        computePostponeArgumentsEdges()
        computeRelatedToAllOutputTypes()
        computeRelatedToTopLevelType()
    }

    fun isVariableRelatedToTopLevelType(variable: TypeConstructorMarker) = relatedToTopLevelType.contains(variable)
    fun isVariableRelatedToAnyOutputType(variable: TypeConstructorMarker) = relatedToAllOutputTypes.contains(variable)

    private fun computeConstraintEdges() {
        fun addConstraintEdge(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            constrainEdges.getOrPut(from) { hashSetOf() }.add(to)
            constrainEdges.getOrPut(to) { hashSetOf() }.add(from)
        }

        for (variableWithConstraints in notFixedTypeVariables.values) {
            val from = variableWithConstraints.typeVariable.freshTypeConstructor(typeSystemContext)

            for (constraint in variableWithConstraints.constraints) {
                constraint.type.forAllMyTypeVariables {
                    if (isMyTypeVariable(it)) {
                        addConstraintEdge(from, it)
                    }
                }
            }
        }
    }

    private fun computePostponeArgumentsEdges() {
        fun addPostponeArgumentsEdges(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            postponeArgumentsEdges.getOrPut(from) { hashSetOf() }.add(to)
        }

        for (argument in postponedKtPrimitives) {
            if (argument.analyzed) continue

            val typeVariablesInOutputType = SmartSet.create<TypeConstructorMarker>()
            (argument.outputType ?: continue).forAllMyTypeVariables { typeVariablesInOutputType.add(it) }
            if (typeVariablesInOutputType.isEmpty()) continue

            for (inputType in argument.inputTypes) {
                inputType.forAllMyTypeVariables { from ->
                    for (to in typeVariablesInOutputType) {
                        addPostponeArgumentsEdges(from, to)
                    }
                }
            }
        }
    }

    private fun computeRelatedToAllOutputTypes() {
        for (argument in postponedKtPrimitives) {
            if (argument.analyzed) continue
            (argument.outputType ?: continue).forAllMyTypeVariables {
                addAllRelatedNodes(relatedToAllOutputTypes, it, includePostponedEdges = false)
            }
        }
    }

    private fun computeRelatedToTopLevelType() {
        if (topLevelType == null) return
        topLevelType.forAllMyTypeVariables {
            addAllRelatedNodes(relatedToTopLevelType, it, includePostponedEdges = true)
        }
    }

    private fun isMyTypeVariable(typeConstructor: TypeConstructorMarker) = notFixedTypeVariables.containsKey(typeConstructor)

    private fun KotlinTypeMarker.forAllMyTypeVariables(action: (TypeConstructorMarker) -> Unit) =
        with(typeSystemContext) {
            contains {
                if (isMyTypeVariable(it.typeConstructor())) action(it.typeConstructor())
                false
            }
        }


    private fun getConstraintEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = constrainEdges[from] ?: emptySet()
    private fun getPostponeEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = postponeArgumentsEdges[from] ?: emptySet()

    private fun addAllRelatedNodes(to: MutableSet<TypeConstructorMarker>, node: TypeConstructorMarker, includePostponedEdges: Boolean) {
        if (to.add(node)) {
            for (relatedNode in getConstraintEdges(node)) {
                addAllRelatedNodes(to, relatedNode, includePostponedEdges)
            }
            if (includePostponedEdges) {
                for (relatedNode in getPostponeEdges(node)) {
                    addAllRelatedNodes(to, relatedNode, includePostponedEdges)
                }
            }
        }
    }


}