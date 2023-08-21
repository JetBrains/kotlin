/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.SmartSet

class TypeVariableDependencyInformationProvider(
    private val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>,
    private val postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
    private val topLevelType: KotlinTypeMarker?,
    private val typeSystemContext: VariableFixationFinder.Context
) {

    private val outerTypeVariables: Set<TypeConstructorMarker>? =
        typeSystemContext.outerTypeVariables

    /*
     * Not oriented edges
     * TypeVariable(A) has UPPER(Function1<TypeVariable(B), R>) => A and B are related deeply
     */
    private val deepTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    /*
     * Not oriented edges
     * TypeVariable(A) has UPPER(TypeVariable(B)) => A and B are related shallowly
     */
    private val shallowTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    // Oriented edges
    private val postponeArgumentsEdges: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    private val relatedToAllOutputTypes: MutableSet<TypeConstructorMarker> = hashSetOf()
    private val relatedToTopLevelType: MutableSet<TypeConstructorMarker> = hashSetOf()

    init {
        computeConstraintEdges()
        computePostponeArgumentsEdges()
        computeRelatedToAllOutputTypes()
        computeRelatedToTopLevelType()
    }

    fun isVariableRelatedToTopLevelType(variable: TypeConstructorMarker) =
        relatedToTopLevelType.contains(variable)


    fun isRelatedToOuterTypeVariable(variable: TypeConstructorMarker): Boolean {
        val outerTypeVariables = outerTypeVariables ?: return false
        val myDependent = getDeeplyDependentVariables(variable) ?: return false
        return myDependent.any { it in outerTypeVariables }
    }

    fun isVariableRelatedToAnyOutputType(variable: TypeConstructorMarker) = relatedToAllOutputTypes.contains(variable)

    fun getDeeplyDependentVariables(variable: TypeConstructorMarker) = deepTypeVariableDependencies[variable]
    fun getShallowlyDependentVariables(variable: TypeConstructorMarker) = shallowTypeVariableDependencies[variable]

    fun areVariablesDependentShallowly(a: TypeConstructorMarker, b: TypeConstructorMarker): Boolean {
        if (a == b) return true

        val shallowDependencies = shallowTypeVariableDependencies[a] ?: return false

        return shallowDependencies.any { it == b } ||
                shallowTypeVariableDependencies.values.any { dependencies -> a in dependencies && b in dependencies }
    }

    private fun computeConstraintEdges() {
        fun addConstraintEdgeForDeepDependency(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            deepTypeVariableDependencies.getOrPut(from) { linkedSetOf() }.add(to)
            deepTypeVariableDependencies.getOrPut(to) { linkedSetOf() }.add(from)
        }

        fun addConstraintEdgeForShallowDependency(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            shallowTypeVariableDependencies.getOrPut(from) { linkedSetOf() }.add(to)
            shallowTypeVariableDependencies.getOrPut(to) { linkedSetOf() }.add(from)
        }

        for (variableWithConstraints in notFixedTypeVariables.values) {
            val from = variableWithConstraints.typeVariable.freshTypeConstructor(typeSystemContext)

            for (constraint in variableWithConstraints.constraints) {
                val constraintTypeConstructor = constraint.type.typeConstructor(typeSystemContext)

                constraint.type.forAllMyTypeVariables {
                    if (isMyTypeVariable(it)) {
                        addConstraintEdgeForDeepDependency(from, it)
                    }
                }
                if (isMyTypeVariable(constraintTypeConstructor)) {
                    addConstraintEdgeForShallowDependency(from, constraintTypeConstructor)
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
                val typeConstructor = it.typeConstructor()
                if (isMyTypeVariable(typeConstructor)) action(typeConstructor)
                false
            }
        }


    private fun getConstraintEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = deepTypeVariableDependencies[from] ?: emptySet()
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
