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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.boundClosure
import org.jetbrains.kotlin.types.typeUtil.constituentTypes
import org.jetbrains.kotlin.utils.DFS

object NonExpansiveInheritanceRestrictionChecker {
    @JvmStatic
    fun check(
            declaration: KtClass,
            classDescriptor: ClassDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val typeConstructor = classDescriptor.typeConstructor
        if (typeConstructor.parameters.isEmpty()) return

        val builder = GraphBuilder(typeConstructor)
        val graph = builder.build()

        val edgesInCycles = graph.expansiveEdges.filter { graph.isEdgeInCycle(it) }
        if (edgesInCycles.isEmpty()) return

        val problemNodes = edgesInCycles.flatMap { setOf(it.from, it.to) }

        for (typeParameter in typeConstructor.parameters) {
            if (typeParameter in problemNodes) {
                val element = DescriptorToSourceUtils.descriptorToDeclaration(typeParameter) ?: declaration
                diagnosticHolder.report(Errors.EXPANSIVE_INHERITANCE.on(element))
                return
            }
        }

        if (problemNodes.any { it.source != SourceElement.NO_SOURCE }) return

        val typeFqNames = problemNodes.map { it.containingDeclaration }.map { it.fqNameUnsafe.asString() }.toSortedSet()
        diagnosticHolder.report(Errors.EXPANSIVE_INHERITANCE_IN_JAVA.on(declaration, typeFqNames.joinToString(", ")))
    }

    private class GraphBuilder(val typeConstructor: TypeConstructor) {
        private val processedTypeConstructors = hashSetOf<TypeConstructor>()
        private val expansiveEdges = hashSetOf<ExpansiveEdge<TypeParameterDescriptor>>()
        private val edgeLists = hashMapOf<TypeParameterDescriptor, MutableSet<TypeParameterDescriptor>>()

        fun build(): Graph<TypeParameterDescriptor> {
            doBuildGraph(typeConstructor)

            return object : Graph<TypeParameterDescriptor> {
                override fun getNeighbors(node: TypeParameterDescriptor) = edgeLists[node] ?: emptyList<TypeParameterDescriptor>()
                override val expansiveEdges = this@GraphBuilder.expansiveEdges
            }
        }

        private fun addEdge(from: TypeParameterDescriptor, to: TypeParameterDescriptor, expansive: Boolean = false) {
            edgeLists.getOrPut(from) { linkedSetOf() }.add(to)
            if (expansive) {
                expansiveEdges.add(ExpansiveEdge(from, to))
            }
        }

        private fun doBuildGraph(typeConstructor: TypeConstructor) {
            if (typeConstructor.parameters.isEmpty()) return

            val typeParameters = typeConstructor.parameters

            // For each type parameter T, let ST be the set of all constituent types of all immediate supertypes of the owner of T.
            // If T appears as a constituent type of a simple type argument A in a generic type in ST, add an edge from T
            // to U, where U is the type parameter corresponding to A, and where the edge is non-expansive if A has the form T or T?,
            // the edge is expansive otherwise.
            for (constituentType in constituentTypes(typeConstructor.supertypes)) {
                val constituentTypeConstructor = constituentType.constructor
                if (constituentTypeConstructor !in processedTypeConstructors) {
                    processedTypeConstructors.add(constituentTypeConstructor)
                    doBuildGraph(constituentTypeConstructor)
                }
                if (constituentTypeConstructor.parameters.size != constituentType.arguments.size) continue

                constituentType.arguments.forEachIndexed { i, typeProjection ->
                    if (typeProjection.projectionKind == Variance.INVARIANT) {
                        val constituents = constituentTypes(setOf(typeProjection.type))

                        for (typeParameter in typeParameters) {
                            if (typeParameter.defaultType in constituents || typeParameter.defaultType.makeNullableAsSpecified(true) in constituents) {
                                addEdge(typeParameter, constituentTypeConstructor.parameters[i], !TypeUtils.isTypeParameter(typeProjection.type))
                            }
                        }
                    }
                    else {
                        // Furthermore, if T appears as a constituent type of an element of the B-closure of the set of lower and
                        // upper bounds of a skolem type variable Q in a skolemization of a projected generic type in ST, add an
                        // expanding edge from T to V, where V is the type parameter corresponding to Q.
                        val originalTypeParameter = constituentTypeConstructor.parameters[i]
                        val bounds = hashSetOf<KotlinType>()

                        val substitutor = TypeConstructorSubstitution.create(constituentType).buildSubstitutor()
                        val adaptedUpperBounds = originalTypeParameter.upperBounds.mapNotNull { substitutor.substitute(it, Variance.INVARIANT) }
                        bounds.addAll(adaptedUpperBounds)

                        if (!typeProjection.isStarProjection) {
                            bounds.add(typeProjection.type)
                        }

                        val boundClosure = boundClosure(bounds)
                        val constituentTypes = constituentTypes(boundClosure)
                        for (typeParameter in typeParameters) {
                            if (typeParameter.defaultType in constituentTypes || typeParameter.defaultType.makeNullableAsSpecified(true) in constituentTypes) {
                                addEdge(typeParameter, originalTypeParameter, true)
                            }
                        }
                    }
                }
            }
        }
    }

    private data class ExpansiveEdge<out T>(val from: T, val to: T)

    private interface  Graph<T> {
        fun getNeighbors(node: T): Collection<T>
        val expansiveEdges: Set<ExpansiveEdge<T>>
    }

    private fun <T> Graph<T>.isEdgeInCycle(edge: ExpansiveEdge<T>) = edge.from in collectReachable(edge.to)

    private fun <T> Graph<T>.collectReachable(from: T): List<T> {
        val handler = object : DFS.NodeHandlerWithListResult<T, T>() {
            override fun afterChildren(current: T?) {
                result.add(current)
            }
        }

        val neighbors = object : DFS.Neighbors<T> {
            override fun getNeighbors(current: T): Iterable<T> = this@collectReachable.getNeighbors(current)
        }

        DFS.dfs(listOf(from), neighbors, handler)

        return handler.result()
    }
}
