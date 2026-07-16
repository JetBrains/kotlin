/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.model

import org.jetbrains.kotlin.backend.common.dependencies.AccessibleIndex
import org.jetbrains.kotlin.backend.common.dependencies.AnonymousInitializerIndex
import org.jetbrains.kotlin.backend.common.dependencies.BeginStaticInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.CompositeNode
import org.jetbrains.kotlin.backend.common.dependencies.CompositeNode.Companion.subgraphFlowDescendants
import org.jetbrains.kotlin.backend.common.dependencies.ConstructorLikeIndex
import org.jetbrains.kotlin.backend.common.dependencies.DeclarationIndex
import org.jetbrains.kotlin.backend.common.dependencies.DependencyEdge.Companion.component2
import org.jetbrains.kotlin.backend.common.dependencies.DependencyGraph
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNode.Companion.informationDescendants
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNodeIndex
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNodeIndex.Companion.enclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.FunctionIndex
import org.jetbrains.kotlin.backend.common.dependencies.InformationEdge.Companion.component1
import org.jetbrains.kotlin.backend.common.dependencies.InformationEdge.Companion.component3
import org.jetbrains.kotlin.backend.common.dependencies.InitializationCycleAccessResult
import org.jetbrains.kotlin.backend.common.dependencies.PropertyIndex
import org.jetbrains.kotlin.backend.common.dependencies.model.AnalysisResult.Companion.with
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.isNotPrivate
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.backend.common.dependencies.util.TraversalOrder
import org.jetbrains.kotlin.backend.common.dependencies.util.stronglyConnectedComponents
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import kotlin.collections.forEach
import kotlin.sequences.emptySequence
import kotlin.sequences.forEach

data class AnalysisResult(val type: InitializationCycleAccessResult, val accesses: Set<IrElement>) {
    companion object {
        infix fun InitializationCycleAccessResult.with(accesses: Set<IrElement>): AnalysisResult = AnalysisResult(this, accesses)
    }
}

data class DeadlockNode(val entity: EnclosingEntity<*>) {
    val incoming: MutableSet<DeadlockNode> = mutableSetOf()
    val outgoing: MutableSet<DeadlockNode> = mutableSetOf()

    fun reset() {
        incoming.clear()
        outgoing.clear()
    }
}

class DependencyGraphAnalyzer(val dependencyGraph: DependencyGraph) {

    context(accessingEntity: EnclosingEntity<*>?, cycle: CompositeNode, mutuallyDependentEntities: Set<EnclosingEntity<*>>)
    private fun analyzeTransitively(
        initial: DependencyNodeIndex,
        node: DependencyNodeIndex,
        visited: MutableSet<DependencyNodeIndex> = mutableSetOf()
    ): Sequence<InitializationCycleAccessResult> = sequence {
        if (!visited.add(node)) {
            if (node is DeclarationIndex<*> && node !is FunctionIndex<*>) {
                yield(InitializationCycleAccessResult.CyclicAccess(node))
            }
            return@sequence
        }
        val informationFlow = cycle.informationFlowInto(node)
        for ([from, _, _] in informationFlow) {
            accessingUninitializedEntityAt(from, cycle)?.let {
                yield(it)
                continue
            }
            if (from !in cycle) continue
            val inOrderAccess = when {
                from.enclosingEntity?.let { it == accessingEntity } ?: false ->
                    // In-order references must be allowed
                    from != initial && from.subgraphFlowDescendants().any { it == initial }
                else -> false
            }
            when (val result = from.accessAnalysisResult) {
                null -> continue
                is InitializationCycleAccessResult.ReportedAndPoisoning if inOrderAccess -> when {
                    analyzeTransitively(initial, from, visited.toMutableSet())
                        .any { it is InitializationCycleAccessResult.ReportedAndPoisoning } -> yield(result)
                    else -> continue
                }
                is InitializationCycleAccessResult.ReportedAndPoisoning -> yield(result)
                is InitializationCycleAccessResult.Reported -> {
                    yield(result)
                    yieldAll(analyzeTransitively(initial, from, visited.toMutableSet()))
                }
                else -> yieldAll(analyzeTransitively(initial, from, visited.toMutableSet()))
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
    context(accessingEntity: EnclosingEntity<*>?)
    private fun accessingUninitializedEntityAt(accessedNode: AccessibleIndex, cycle: CompositeNode): InitializationCycleAccessResult? {
        // Even though constructors (may) statically initialize their containing classes, there is no actual access to their (initialized)
        // declarations whatsoever
        if (accessedNode is FunctionIndex.Constructor) return null
        val accessedEntity = accessedNode.enclosingEntity
            ?: accessedNode.lazilyInitialized
            ?: return null
        if (accessedEntity == accessingEntity) return null
        // We consider 2 cases when such accesses might arise:
        return when {
            // If the accessed entity is nested under an interface...
            (accessedEntity.parentEnclosingEntity as? EnclosingEntity.Class)?.let { it in cycle && it.symbol.owner.kind.isInterface } ?: false ->
                InitializationCycleAccessResult.InaccessibleEntityAccess(accessedEntity.parentEnclosingEntity!!, accessedNode)
            // If the accessed entity's static initialization has a beginning in the happens-before cycle (enum entry and inheritance case)...
            accessedEntity.beginInitializationIndex in cycle -> InitializationCycleAccessResult.InaccessibleEntityAccess(accessedEntity, accessedNode)
            else -> null
        }
    }

    fun analyze(node: DependencyNodeIndex): Sequence<AnalysisResult> = (dependencyGraph[node] as? CompositeNode)?.let { cycle ->
        val accessingEntity = node.enclosingEntity
        context(accessingEntity, cycle, accessingEntity?.let { mutuallyDependentEntities(it) } ?: emptySet()) {
            val informationFlow = cycle.informationFlowInto(node)
            sequence {
                for ([from, _, accesses] in informationFlow) {
                    accessingUninitializedEntityAt(from, cycle)?.let {
                        yield(it with accesses)
                        continue
                    }
                    if (from !in cycle) continue
                    val inOrderAccess = when {
                        from.enclosingEntity?.let { it == accessingEntity } ?: false ->
                            // In-order references must be allowed
                            from != node && from.subgraphFlowDescendants().any { it == node }
                        else -> false
                    }
                    when (val result = from.accessAnalysisResult) {
                        null -> continue
                        is InitializationCycleAccessResult.ReportedAndPoisoning if inOrderAccess -> when {
                            analyzeTransitively(node, from, mutableSetOf(node))
                                .any { it is InitializationCycleAccessResult.ReportedAndPoisoning } -> yield(result with accesses)
                            else -> continue
                        }
                        is InitializationCycleAccessResult.ReportedAndPoisoning -> yield(result with accesses)
                        is InitializationCycleAccessResult.Reported -> {
                            yield(result with accesses)
                            analyzeTransitively(node, from, mutableSetOf(node))
                                .forEach { yield(it with accesses) }
                        }
                        else -> analyzeTransitively(node, from, mutableSetOf(node)).forEach { yield(it with accesses) }
                    }
                }
            }
        }
    } ?: emptySequence()

    private val DependencyNodeIndex.canCauseDeadlock: Boolean
        get() = this is PropertyIndex || this is AnonymousInitializerIndex || this is BeginStaticInitializationIndex<*> || this is ConstructorLikeIndex<*>

    private fun Set<EnclosingEntity<*>>.buildDeadlockGraph(): Set<DeadlockNode> {
        val nodes = mutableMapOf<EnclosingEntity<*>, DeadlockNode>()
        // First, initialize all the possible nodes
        forEach { entity ->
            val reportedEntity = entity.parentEnclosingEntityOrSelf
            nodes[reportedEntity] = DeadlockNode(reportedEntity)
        }
        // Second, materialize the deadlocking access dependencies between them
        context(dependencyGraph) {
            this@buildDeadlockGraph.forEach { entity ->
                val reportedEntity = entity.parentEnclosingEntityOrSelf
                val node = nodes[reportedEntity] ?: return@forEach
                // We need to check the nodes of the actual entity, not the reported one
                dependencyGraph[entity].filterIsInstance<AccessibleIndex>().forEach { index ->
                    if (!index.canCauseDeadlock) return@forEach
                    index.informationDescendants { descendant ->
                        val descendantEntity = descendant.enclosingEntity?.parentEnclosingEntityOrSelf ?: return@informationDescendants true
                        val descendantNode = nodes[descendantEntity] ?: return@informationDescendants false
                        descendant.canCauseDeadlock && descendantNode !in node.outgoing
                    }.forEach { descendant ->
                        val descendantEntity = descendant.enclosingEntity?.parentEnclosingEntityOrSelf ?: return@forEach
                        if (descendantEntity == reportedEntity) return@forEach
                        val descendantNode = nodes[descendantEntity] ?: return@forEach
                        if (descendant != index && descendant.canCauseDeadlock) {
                            node.outgoing += descendantNode
                            descendantNode.incoming += node
                        }
                    }
                }
            }
        }
        return nodes.values.toSet()
    }

    private val deadlockingComponentsCache: MutableMap<EnclosingEntity<*>, Set<EnclosingEntity<*>>?> = mutableMapOf()

    private fun CompositeNode.precomputeDeadlockingComponents() {
        val deadlockGraph = enclosingEntities.buildDeadlockGraph()
        val components = deadlockGraph.stronglyConnectedComponents(
            ancestors = { node, visited ->
                TraversalOrder.PreOrder.traverse(
                    start = node,
                    visited = visited,
                    neighbours = { it.incoming.asSequence() }
                )
            },
            descendants = { node, visited ->
                TraversalOrder.PreOrder.traverse(
                    start = node,
                    visited = visited,
                    neighbours = { it.outgoing.asSequence() }
                )
            }
        )
        components.forEach { component ->
            component.takeIf { it.size > 1 }?.forEach { node ->
                deadlockingComponentsCache[node.entity] = component.mapToSetOrEmpty {
                    it.reset()
                    it.entity
                }
            } ?: component.first().let {
                it.reset()
                // Could be the parent entity which was/will be added in a previous/following component
                if (it.entity !in deadlockingComponentsCache) deadlockingComponentsCache[it.entity] = null
            }
        }
    }

    fun mutuallyDependentEntities(enclosingEntity: EnclosingEntity<*>): Set<EnclosingEntity<*>> =
        when {
            enclosingEntity.isPrivate -> emptySet()
            else -> {
                val reportedEntity = enclosingEntity.parentEnclosingEntityOrSelf
                if (reportedEntity in deadlockingComponentsCache) return deadlockingComponentsCache[reportedEntity] ?: emptySet()
                val cycle = dependencyGraph[enclosingEntity].mapNotNull(dependencyGraph::get)
                    .firstIsInstanceOrNull<CompositeNode>() ?: return emptySet()
                if (reportedEntity !in deadlockingComponentsCache) cycle.precomputeDeadlockingComponents()
                deadlockingComponentsCache[reportedEntity]?.asSequence()
                    ?.filter { it.isNotPrivate && it != reportedEntity }?.toSet()
                    ?: emptySet()
            }
        }

    fun reset() {
        deadlockingComponentsCache.clear()
    }
}
