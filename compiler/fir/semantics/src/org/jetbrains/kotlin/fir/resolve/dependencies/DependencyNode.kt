/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNodeIndex.Companion.enclosingEntity
import org.jetbrains.kotlin.fir.util.SetMultimap
import kotlin.collections.forEach
import kotlin.collections.set

internal typealias InformationFlowMap = MutableMap<AccessibleIndex, InformationEdge>

internal fun InformationFlowMap.insertEdge(edge: InformationEdge): Boolean =
    this[edge.from]?.let { prev ->
        if (prev == edge) return@let false
        this[edge.from] = prev.merge(edge.accessSources)
        return@let true
    } ?: (putIfAbsent(edge.from, edge) == null)

internal fun InformationFlowMap.removeEdge(edge: InformationEdge): Boolean = remove(edge.from, edge)

internal typealias HappensBeforeFlowMap = MutableMap<DependencyNodeIndex, HappensBeforeEdge>

internal inline fun HappensBeforeFlowMap.insertEdge(edge: HappensBeforeEdge, key: (DependencyEdge) -> DependencyNodeIndex): Boolean =
    putIfAbsent(key(edge), edge) == null

internal inline fun HappensBeforeFlowMap.removeEdge(edge: HappensBeforeEdge, key: (DependencyEdge) -> DependencyNodeIndex): Boolean =
    remove(key(edge), edge)

sealed class DependencyNode {

    abstract val index: DependencyNodeIndex

    abstract val isComposite: Boolean

    protected val incomingHappensBeforeFlow: HappensBeforeFlowMap = mutableMapOf()

    protected val outgoingHappensBeforeFlow: HappensBeforeFlowMap = mutableMapOf()

    val happensBeforeFlow: Sequence<HappensBeforeEdge> get() = incomingHappensBeforeFlow.asSequence().map { it.value }

    val happensAfterFlow: Sequence<HappensBeforeEdge> get() = outgoingHappensBeforeFlow.asSequence().map { it.value }

    context(graph: DependencyGraph)
    val happenBefore: Sequence<DependencyNode> get() = incomingHappensBeforeFlow.asSequence().mapNotNull { graph[it.key] }

    context(graph: DependencyGraph)
    val happenAfter: Sequence<DependencyNode> get() = outgoingHappensBeforeFlow.asSequence().mapNotNull { graph[it.key] }

    open fun insertIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> incomingHappensBeforeFlow.insertEdge(edge, DependencyEdge::from)
            else -> false
        }

    open fun removeIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> incomingHappensBeforeFlow.removeEdge(edge, DependencyEdge::from)
            else -> false
        }

    open fun insertOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> outgoingHappensBeforeFlow.insertEdge(edge, DependencyEdge::to)
            else -> false
        }

    open fun removeOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> outgoingHappensBeforeFlow.removeEdge(edge, DependencyEdge::to)
            else -> false
        }

    open fun reset() {
        incomingHappensBeforeFlow.clear()
        outgoingHappensBeforeFlow.clear()
    }

    companion object {

        context(graph: DependencyGraph)
        internal inline fun DependencyNode.happensBeforeAncestors(
            visited: MutableSet<DependencyNode> = mutableSetOf(),
            traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
            crossinline predicate: (DependencyNode) -> Boolean = { true }
        ): Sequence<DependencyNode> =
            traversalOrder.traverse(
                start = this@happensBeforeAncestors,
                visited = visited,
                predicate = predicate,
                neighbours = { it.happenBefore }
            )

        context(graph: DependencyGraph)
        internal inline fun DependencyNode.happensBeforeDescendants(
            visited: MutableSet<DependencyNode> = mutableSetOf(),
            traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
            crossinline predicate: (DependencyNode) -> Boolean = { true }
        ): Sequence<DependencyNode> =
            traversalOrder.traverse(
                start = this@happensBeforeDescendants,
                visited = visited,
                predicate = predicate,
                neighbours = { it.happenAfter }
            )
    }
}

data class UnitNode(override val index: DependencyNodeIndex) : DependencyNode() {
    val enclosingEntity: EnclosingEntity<*>? = index.enclosingEntity
    private val incomingInformationFlow: InformationFlowMap = mutableMapOf()

    val informationFlow: Sequence<InformationEdge> get() = incomingInformationFlow.asSequence().map { it.value }

    override val isComposite: Boolean = false

    override fun insertIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                incomingHappensBeforeFlow.insertEdge(edge, DependencyEdge::from)
                incomingInformationFlow.insertEdge(edge)
            }
            is InformationEdge -> incomingInformationFlow.insertEdge(edge)
            else -> super.insertIncomingEdge(edge)
        }

    override fun removeIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                incomingHappensBeforeFlow.removeEdge(edge, DependencyEdge::from)
                incomingInformationFlow.removeEdge(edge)
            }
            is InformationEdge -> incomingInformationFlow.removeEdge(edge)
            else -> super.removeIncomingEdge(edge)
        }

    override fun reset() {
        super.reset()
        incomingInformationFlow.clear()
    }
}

data class CompositeNode(
    private val indices: Set<DependencyNodeIndex>,
    private val entities: SetMultimap<EnclosingEntity<*>, DependencyNodeIndex>,
    private val subgraphFlow: SetMultimap<DependencyNodeIndex, HappensBeforeEdge>,
) : DependencyNode(), Set<DependencyNodeIndex> by indices {

    val enclosingEntities: Set<EnclosingEntity<*>> get() = entities.keys

    private val incomingInformationFlow: MutableMap<DependencyNodeIndex, InformationFlowMap> = mutableMapOf()

    fun informationFlowInto(index: DependencyNodeIndex): Sequence<InformationEdge> =
        incomingInformationFlow[index]?.asSequence()?.map { it.value } ?: emptySequence()

    fun subgraphFlowFrom(index: DependencyNodeIndex): Sequence<HappensBeforeEdge> = subgraphFlow[index].asSequence()

    operator fun get(enclosingEntity: EnclosingEntity<*>): Sequence<DependencyNodeIndex> = entities[enclosingEntity].asSequence()

    operator fun contains(enclosingEntity: EnclosingEntity<*>): Boolean = enclosingEntity in enclosingEntities

    override operator fun contains(element: DependencyNodeIndex): Boolean =
        when (element) {
            is CompositeIndex -> element.indices.any { it in indices }
            else -> element in indices
        }

    override val index: CompositeIndex = CompositeIndex(indices)
    override val isComposite: Boolean = true

    override fun insertIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                if (edge.from !in this) incomingHappensBeforeFlow.insertEdge(edge, DependencyEdge::from)
                incomingInformationFlow.getOrPut(edge.to) { mutableMapOf() }.insertEdge(edge)
            }
            is InformationEdge -> incomingInformationFlow.getOrPut(edge.to) { mutableMapOf() }.insertEdge(edge)
            else -> super.insertIncomingEdge(edge)
        }

    override fun removeIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                if (edge.from !in this) incomingHappensBeforeFlow.removeEdge(edge, DependencyEdge::from)
                incomingInformationFlow[edge.from]?.let {
                    val result = it.removeEdge(edge)
                    if (it.isEmpty()) incomingInformationFlow.remove(edge.from)
                    result
                } ?: false
            }
            is InformationEdge -> incomingInformationFlow[edge.from]?.let {
                val result = it.removeEdge(edge)
                if (it.isEmpty()) incomingInformationFlow.remove(edge.from)
                result
            } ?: false
            else -> super.removeIncomingEdge(edge)
        }

    override fun insertOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy if edge.to !in this -> outgoingHappensBeforeFlow.insertEdge(edge, DependencyEdge::to)
            is HappensBeforeEdge -> outgoingHappensBeforeFlow.insertEdge(edge, DependencyEdge::to)
            else -> false
        }

    override fun removeOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy if edge.to !in this -> outgoingHappensBeforeFlow.removeEdge(edge, DependencyEdge::to)
            is HappensBeforeEdge -> outgoingHappensBeforeFlow.removeEdge(edge, DependencyEdge::to)
            else -> false
        }

    override fun reset() {
        super.reset()
        incomingInformationFlow.forEach { it.value.clear() }
        incomingInformationFlow.clear()
    }

    companion object {
        context(cycle: CompositeNode)
        fun DependencyNodeIndex.subgraphFlowDescendants(): Sequence<DependencyNodeIndex> =
            TraversalOrder.PreOrder.traverse(
                start = this@subgraphFlowDescendants,
                predicate = { it in cycle },
                neighbours = {
                    cycle.subgraphFlowFrom(it).flatMap { edge ->
                        if (edge.to is CompositeIndex) {
                            (edge.to as CompositeIndex).indices
                        } else {
                            setOf(edge.to)
                        }
                    }
                }
            )
    }
}
