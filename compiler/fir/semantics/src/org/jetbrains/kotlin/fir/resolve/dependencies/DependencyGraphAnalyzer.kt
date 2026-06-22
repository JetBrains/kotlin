/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyEdge.Companion.component2
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.isNotPrivate
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.fir.resolve.dependencies.InformationEdge.Companion.component1
import org.jetbrains.kotlin.fir.resolve.dependencies.InformationEdge.Companion.component3

class DependencyGraphAnalyzer(val dependencyGraph: DependencyGraph) : FirSessionComponent {

    private val poisonedNodes = mutableSetOf<DependencyNodeIndex>()

    private fun checkPoisoning(
        node: DependencyNodeIndex,
        cycle: CompositeNode,
        visited: MutableSet<DependencyNodeIndex> = mutableSetOf()
    ): Boolean {
        if (!visited.add(node)) return true
        val informationFlow = cycle.informationFlowInto(node)
        for ([from, _, _] in informationFlow) {
            val accessingUninitializedEntity = accessingUninitializedEntityAt(from, cycle)
            if (from !in cycle && !accessingUninitializedEntity) continue
            when (from.traversalAction) {
                CyclicAccessTraversalAction.POSSIBLY_UNINITIALIZED -> return true
                CyclicAccessTraversalAction.TRANSITIVELY_CONTINUE -> {
                    if (accessingUninitializedEntity) return true
                    if (checkPoisoning(from, cycle, visited.toMutableSet())) return true
                }
                CyclicAccessTraversalAction.IGNORE -> return accessingUninitializedEntity
            }
        }
        return false
    }

    private fun accessingUninitializedEntityAt(index: AccessibleIndex, cycle: CompositeNode): Boolean {
        val enclosingEntity = (index as? StaticInitializationIndex)?.enclosingEntity ?: index.lazilyInitialized ?: return false
        return enclosingEntity in cycle && ((enclosingEntity.parentEnclosingEntity as? EnclosingEntity.Class)?.let { parent ->
            parent in cycle && (parent.symbol.isInterface || parent.beginInitializationIndex in cycle)
        } ?: false || enclosingEntity.beginInitializationIndex in cycle)
    }

    fun collectAllPoisoningDirectAccesses(node: DependencyNodeIndex): Set<KtSourceElement> {
        require(node in poisonedNodes) { "Node $node must be poisoned in order to retrieve its poisoning accesses!" }
        return (dependencyGraph[node] as? CompositeNode)?.let { cycle ->
            val informationFlow = cycle.informationFlowInto(node)
            mutableSetOf<KtSourceElement>().apply {
                for ([from, _, accesses] in informationFlow) {
                    val accessingUninitializedEntity = accessingUninitializedEntityAt(from, cycle)
                    if (from !in cycle && !accessingUninitializedEntity) continue
                    when (from.traversalAction) {
                        CyclicAccessTraversalAction.POSSIBLY_UNINITIALIZED -> addAll(accesses)
                        CyclicAccessTraversalAction.TRANSITIVELY_CONTINUE -> {
                            if (accessingUninitializedEntity) addAll(accesses)
                            if (checkPoisoning(from, cycle, mutableSetOf(node))) addAll(accesses)
                        }
                        CyclicAccessTraversalAction.IGNORE -> if (accessingUninitializedEntity) addAll(accesses)
                    }
                }
            }
        } ?: emptySet()
    }

    fun isPoisoned(index: DependencyNodeIndex): Boolean {
        if (index !in dependencyGraph) return false
        if (index in poisonedNodes) return true
        return (dependencyGraph[index] as? CompositeNode)?.let { node ->
            if (checkPoisoning(index, node)) {
                poisonedNodes.add(index)
                true
            } else false
        } ?: false
    }

    fun mutuallyDependentEntities(enclosingEntity: EnclosingEntity<*>): Sequence<EnclosingEntity<*>> =
        dependencyGraph[enclosingEntity].mapNotNull(dependencyGraph::get)
            .filterIsInstance<CompositeNode>()
            .flatMap { it.enclosingEntities }
            .map { it.parentEnclosingEntityOrSelf }
            .filter { it.isNotPrivate && it != enclosingEntity }
            .distinct()

    fun clear() {
        poisonedNodes.clear()
    }
}

val FirSession.dependencyGraphAnalyzer: DependencyGraphAnalyzer? by FirSession.nullableSessionComponentAccessor()
