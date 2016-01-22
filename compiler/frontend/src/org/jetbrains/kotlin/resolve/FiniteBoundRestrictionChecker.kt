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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.boundClosure
import org.jetbrains.kotlin.types.typeUtil.constituentTypes
import org.jetbrains.kotlin.utils.DFS

object FiniteBoundRestrictionChecker {
    @JvmStatic
    fun check(
            declaration: KtClass,
            classDescriptor: ClassDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val typeConstructor = classDescriptor.typeConstructor
        if (typeConstructor.parameters.isEmpty()) return

        // For every projection type argument A in every generic type B<…> in the set of constituent types
        // of every type in the B-closure the set of declared upper bounds of every type parameter T add an
        // edge from T to U, where U is the type parameter of the declaration of B<…> corresponding to the type argument A.
        // It is a compile-time error if the graph G has a cycle.
        val graph = GraphBuilder(typeConstructor).build()

        val problemNodes = graph.nodes.filter { graph.isInCycle(it) }
        if (problemNodes.isEmpty()) return

        for (typeParameter in typeConstructor.parameters) {
            if (typeParameter in problemNodes) {
                val element = DescriptorToSourceUtils.descriptorToDeclaration(typeParameter) ?: declaration
                diagnosticHolder.report(Errors.FINITE_BOUNDS_VIOLATION.on(element))
                return
            }
        }

        if (problemNodes.any { it.source != SourceElement.NO_SOURCE }) return

        val typeFqNames = problemNodes.map { it.containingDeclaration }.map { it.fqNameUnsafe.asString() }.toSortedSet()
        diagnosticHolder.report(Errors.FINITE_BOUNDS_VIOLATION_IN_JAVA.on(declaration, typeFqNames.joinToString(", ")))
    }

    private class GraphBuilder(val typeConstructor: TypeConstructor) {
        private val nodes: MutableSet<TypeParameterDescriptor> = hashSetOf()
        private val edgeLists = hashMapOf<TypeParameterDescriptor, MutableList<TypeParameterDescriptor>>()
        private val processedTypeConstructors = hashSetOf<TypeConstructor>()

        fun build(): Graph<TypeParameterDescriptor> {
            buildGraph(typeConstructor)

            return object : Graph<TypeParameterDescriptor> {
                override val nodes = this@GraphBuilder.nodes
                override fun getNeighbors(node: TypeParameterDescriptor) = edgeLists[node] ?: emptyList<TypeParameterDescriptor>()
            }
        }

        private fun addEdge(from: TypeParameterDescriptor, to: TypeParameterDescriptor) = edgeLists.getOrPut(from) { arrayListOf() }.add(to)

        private fun buildGraph(typeConstructor: TypeConstructor) {
            typeConstructor.parameters.forEach { typeParameter ->
                val boundClosure = boundClosure(typeParameter.upperBounds)
                val constituentTypes = constituentTypes(boundClosure)
                for (constituentType in constituentTypes) {
                    val constituentTypeConstructor = constituentType.constructor
                    if (constituentTypeConstructor !in processedTypeConstructors) {
                        processedTypeConstructors.add(constituentTypeConstructor)
                        buildGraph(constituentTypeConstructor)
                    }
                    if (constituentTypeConstructor.parameters.size != constituentType.arguments.size) continue

                    constituentType.arguments.forEachIndexed { i, typeProjection ->
                        if (typeProjection.projectionKind != Variance.INVARIANT) {
                            nodes.add(typeParameter)
                            nodes.add(constituentTypeConstructor.parameters[i])
                            addEdge(typeParameter, constituentTypeConstructor.parameters[i])
                        }
                    }
                }
            }
        }
    }

    private interface  Graph<T> {
        val nodes: Set<T>
        fun getNeighbors(node: T): List<T>
    }

    private fun <T> Graph<T>.isInCycle(from: T): Boolean {
        var result = false

        val visited = object : DFS.VisitedWithSet<T>() {
            override fun checkAndMarkVisited(current: T): Boolean {
                val added = super.checkAndMarkVisited(current)
                if (!added && current == from) {
                    result = true
                }
                return added
            }

        }

        val handler = object : DFS.AbstractNodeHandler<T, Unit>() {
            override fun result() {}
        }

        val neighbors = object : DFS.Neighbors<T> {
            override fun getNeighbors(current: T) = this@isInCycle.getNeighbors(current)
        }

        DFS.dfs(listOf(from), neighbors, visited, handler)

        return result
    }
}
