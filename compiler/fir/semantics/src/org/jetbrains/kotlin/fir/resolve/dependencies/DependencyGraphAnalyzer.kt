/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.resolve.dependencies.AnalysisResult.Companion.with
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyEdge.Companion.component2
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.isNotPrivate
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.fir.resolve.dependencies.InformationEdge.Companion.component1
import org.jetbrains.kotlin.fir.resolve.dependencies.InformationEdge.Companion.component3

data class AnalysisResult(val type: InitializationCycleAccessResult, val accesses: Set<KtSourceElement>) {
    companion object {
        infix fun InitializationCycleAccessResult.with(accesses: Set<KtSourceElement>): AnalysisResult = AnalysisResult(this, accesses)
    }
}

class DependencyGraphAnalyzer(val dependencyGraph: DependencyGraph) : FirSessionComponent {

    context(accessingEntity: EnclosingEntity<*>?, cycle: CompositeNode)
    private fun analyze(
        node: DependencyNodeIndex,
        visited: MutableSet<DependencyNodeIndex> = mutableSetOf()
    ): Sequence<InitializationCycleAccessResult> = sequence {
        if (!visited.add(node)) {
            (node as? DeclarationIndex<*>)?.takeIf { it is AccessibleIndex && !it.accessAnalysisResult.poisonsInitializers }
                ?.let { yield(InitializationCycleAccessResult.CyclicAccess(it)) }
            return@sequence
        }
        val informationFlow = cycle.informationFlowInto(node)
        for ([from, _, _] in informationFlow) {
            accessingUninitializedEntityAt(from, cycle)?.let {
                yield(it)
                continue
            }
            if (from !in cycle) continue
            when (val result = from.accessAnalysisResult) {
                is InitializationCycleAccessResult.DeadlockInducingConstructorCall -> {
                    yield(result)
                    yieldAll(analyze(from, visited.toMutableSet()))
                }
                InitializationCycleAccessResult.PropagatesTransitiveDependencies -> yieldAll(analyze(from, visited.toMutableSet()))
                InitializationCycleAccessResult.Safe -> continue
                else -> yield(result)
            }
        }
    }

    /**
     * Checks whether the [accessedNode] belongs to/is nested under an entity whose (singleton) instance is inaccessible by its recursively
     * initialized entities, assuming to the Kotlin's JVM compilation scheme.
     *
     * In general, `A.foo()` (where A is a (companion) object or the A's qualifier resolves a companion object of A) is an uninitialized
     * access iff access to the singleton instance of A (or its resolved object) yields null. If that is the case, such accesses will
     * always result in a `ExceptionInInitializerError` reporting an NPE on the singleton.
     *
     * In JVM terms, when accessing the singleton instance yields null, it is due to the fact that its instance field has not been
     * assigned yet, i.e. the access must have happened during the execution of its corresponding class' `<clinit>` and before its instance
     * is assigned to an accessible field available at the parent (class) entity (during its own `<clinit>`). This important because when
     * a companion object is nested under an interface, the compilation scheme defines a `<clinit>` for the companion object class that
     * initializes the singleton object instance but gets invoked BEFORE it is assigned to the field. The same goes for enum entries and
     * companion objects in enum classes.
     */
    private fun accessingUninitializedEntityAt(accessedNode: AccessibleIndex, cycle: CompositeNode): InitializationCycleAccessResult? {
        // Even though constructors (may) statically initialize their containing classes, there is no actual access to their (initialized)
        // declarations whatsoever
        if (accessedNode is FunctionIndex.Constructor) return null
        val accessedEntity = (accessedNode as? StaticInitializationIndex)?.enclosingEntity
            ?: accessedNode.lazilyInitialized
            ?: return null
        // We consider 2 cases when such accesses might arise:
        return when {
            // If the accessed entity is nested under an interface...
            (accessedEntity.parentEnclosingEntity as? EnclosingEntity.Class)?.let { it in cycle && it.symbol.isInterface } ?: false ->
                InitializationCycleAccessResult.InaccessibleEntityAccess(accessedEntity.parentEnclosingEntity!!, accessedNode)
            // If the accessed entity's static initialization has a beginning in the happens-before cycle (enum entry and inheritance case)...
            accessedEntity.beginInitializationIndex in cycle -> InitializationCycleAccessResult.InaccessibleEntityAccess(accessedEntity, accessedNode)
            else -> null
        }
    }

    fun performAccessAnalysis(node: StaticInitializationIndex): Sequence<AnalysisResult> =
        (dependencyGraph[node] as? CompositeNode)?.let { cycle ->
            context(node.enclosingEntity, cycle) {
                val informationFlow = cycle.informationFlowInto(node)
                sequence {
                    for ([from, _, accesses] in informationFlow) {
                        accessingUninitializedEntityAt(from, cycle)?.let {
                            yield(it with accesses)
                            continue
                        }
                        if (from !in cycle) continue
                        when (val result = from.accessAnalysisResult) {
                            is InitializationCycleAccessResult.DeadlockInducingConstructorCall -> {
                                yield(result with accesses)
                                analyze(from, mutableSetOf(node)).forEach { yield(it with accesses) }
                            }
                            InitializationCycleAccessResult.PropagatesTransitiveDependencies ->
                                analyze(from, mutableSetOf(node)).forEach { yield(it with accesses) }
                            InitializationCycleAccessResult.Safe -> continue
                            else -> yield(result with accesses)
                        }
                    }
                }
            }
        } ?: emptySequence()

    fun mutuallyDependentEntities(enclosingEntity: EnclosingEntity<*>): Sequence<EnclosingEntity<*>> =
        when {
            enclosingEntity.isPrivate -> emptySequence()
            else -> dependencyGraph[enclosingEntity].mapNotNull(dependencyGraph::get)
                .filterIsInstance<CompositeNode>()
                .flatMap { it.enclosingEntities }
                .map { it.parentEnclosingEntityOrSelf }
                .filter { it.isNotPrivate && it != enclosingEntity }
                .distinct()
        }
}

val FirSession.dependencyGraphAnalyzer: DependencyGraphAnalyzer? by FirSession.nullableSessionComponentAccessor()
