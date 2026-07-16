/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies

import org.jetbrains.kotlin.backend.common.dependencies.DependencyNodeIndex.Companion.enclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.util.SetMultimap
import org.jetbrains.kotlin.backend.common.dependencies.util.TraversalOrder
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.let

sealed class DependencyFlowMap<E : DependencyEdge> : MutableMap<DependencyNodeIndex, E> by mutableMapOf() {

    abstract fun insertEdge(edge: E, key: (E) -> DependencyNodeIndex): Boolean

    inline fun removeEdge(edge: E, key: (E) -> DependencyNodeIndex): Boolean = remove(key(edge), edge)

    class HappensBeforeFlowMap : DependencyFlowMap<HappensBeforeEdge>() {
        @Suppress("OVERRIDE_BY_INLINE")
        override inline fun insertEdge(edge: HappensBeforeEdge, key: (HappensBeforeEdge) -> DependencyNodeIndex): Boolean =
            putIfAbsent(key(edge), edge) == null
    }

    class InformationFlowMap : DependencyFlowMap<InformationEdge>() {
        @Suppress("OVERRIDE_BY_INLINE")
        override inline fun insertEdge(edge: InformationEdge, key: (InformationEdge) -> DependencyNodeIndex): Boolean {
            val key = key(edge)
            return this[key]?.let { prev ->
                prev.merge(edge)?.let {
                    this[key] = it
                    true
                } ?: false
            } ?: (putIfAbsent(key, edge) == null)
        }
    }
}

sealed class DependencyNode {

    abstract val index: DependencyNodeIndex

    abstract val isComposite: Boolean

    protected val incomingHappensBeforeFlowMap: DependencyFlowMap.HappensBeforeFlowMap = DependencyFlowMap.HappensBeforeFlowMap()

    protected val outgoingHappensBeforeFlowMap: DependencyFlowMap.HappensBeforeFlowMap = DependencyFlowMap.HappensBeforeFlowMap()

    val incomingHappensBeforeFlow: Sequence<HappensBeforeEdge> get() = incomingHappensBeforeFlowMap.asSequence().map { it.value }

    val outgoingHappensBeforeFlow: Sequence<HappensBeforeEdge> get() = outgoingHappensBeforeFlowMap.asSequence().map { it.value }

    abstract val incomingInformationFlow: Sequence<InformationEdge>

    abstract val outgoingInformationFlow: Sequence<InformationEdge>

    context(graph: DependencyGraph)
    val happenBefore: Sequence<DependencyNode> get() = incomingHappensBeforeFlowMap.asSequence().mapNotNull { graph[it.key] }

    context(graph: DependencyGraph)
    val happenAfter: Sequence<DependencyNode> get() = outgoingHappensBeforeFlowMap.asSequence().mapNotNull { graph[it.key] }

    open fun insertIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> incomingHappensBeforeFlowMap.insertEdge(edge, DependencyEdge::from)
            else -> false
        }

    open fun removeIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> incomingHappensBeforeFlowMap.removeEdge(edge, DependencyEdge::from)
            else -> false
        }

    open fun insertOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> outgoingHappensBeforeFlowMap.insertEdge(edge, DependencyEdge::to)
            else -> false
        }

    open fun removeOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is HappensBeforeEdge -> outgoingHappensBeforeFlowMap.removeEdge(edge, DependencyEdge::to)
            else -> false
        }

    open fun reset() {
        incomingHappensBeforeFlowMap.clear()
        outgoingHappensBeforeFlowMap.clear()
    }

    companion object {

        context(graph: DependencyGraph)
        internal inline fun DependencyNode.happensBeforeAncestors(
            visited: MutableSet<DependencyNode> = mutableSetOf(),
            traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
            crossinline predicate: (DependencyNode) -> Boolean = { true }
        ): Sequence<DependencyNode> =
            traversalOrder.traverse(
                start = this,
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
                start = this,
                visited = visited,
                predicate = predicate,
                neighbours = { it.happenAfter }
            )

        context(graph: DependencyGraph)
        internal inline fun DependencyNodeIndex.informationAncestors(
            visited: MutableSet<DependencyNodeIndex> = mutableSetOf(),
            traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
            crossinline predicate: (DependencyNodeIndex) -> Boolean = { true },
        ): Sequence<DependencyNodeIndex> =
            traversalOrder.traverse(
                start = this,
                visited = visited,
                predicate = predicate,
                neighbours = {
                    when (val node = graph[it]) {
                        null -> emptySequence()
                        is UnitNode -> node.incomingInformationFlow.map(DependencyEdge::from)
                        is CompositeNode -> node.informationFlowInto(it).map(DependencyEdge::from)
                    }
                }
            )

        context(graph: DependencyGraph)
        internal inline fun DependencyNodeIndex.informationDescendants(
            visited: MutableSet<DependencyNodeIndex> = mutableSetOf(),
            traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
            crossinline predicate: (DependencyNodeIndex) -> Boolean = { true },
        ): Sequence<DependencyNodeIndex> =
            traversalOrder.traverse(
                start = this,
                visited = visited,
                predicate = predicate,
                neighbours = {
                    when (val node = graph[it]) {
                        null -> emptySequence()
                        is UnitNode -> node.outgoingInformationFlow.map(DependencyEdge::to)
                        is CompositeNode -> node.informationFlowFrom(it).map(DependencyEdge::to)
                    }
                }
            )
    }
}

data class UnitNode(override val index: DependencyNodeIndex) : DependencyNode() {
    val enclosingEntity: EnclosingEntity<*>? = index.enclosingEntity
    private val incomingInformationFlowMap: DependencyFlowMap.InformationFlowMap = DependencyFlowMap.InformationFlowMap()

    private val outgoingInformationFlowMap: DependencyFlowMap.InformationFlowMap = DependencyFlowMap.InformationFlowMap()

    override val incomingInformationFlow: Sequence<InformationEdge> get() = incomingInformationFlowMap.asSequence().map { it.value }

    override val outgoingInformationFlow: Sequence<InformationEdge> get() = outgoingInformationFlowMap.asSequence().map { it.value }

    override val isComposite: Boolean = false

    override fun insertIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                incomingHappensBeforeFlowMap.insertEdge(edge, DependencyEdge::from)
                incomingInformationFlowMap.insertEdge(edge, DependencyEdge::from)
            }
            is InformationEdge -> incomingInformationFlowMap.insertEdge(edge, DependencyEdge::from)
            else -> super.insertIncomingEdge(edge)
        }

    override fun removeIncomingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                incomingHappensBeforeFlowMap.removeEdge(edge, DependencyEdge::from)
                incomingInformationFlowMap.removeEdge(edge, DependencyEdge::from)
            }
            is InformationEdge -> incomingInformationFlowMap.removeEdge(edge, DependencyEdge::from)
            else -> super.removeIncomingEdge(edge)
        }

    override fun insertOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                outgoingHappensBeforeFlowMap.insertEdge(edge, DependencyEdge::to)
                outgoingInformationFlowMap.insertEdge(edge, DependencyEdge::to)
            }
            is InformationEdge -> outgoingInformationFlowMap.insertEdge(edge, DependencyEdge::to)
            else -> super.insertOutgoingEdge(edge)
        }

    override fun removeOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                outgoingHappensBeforeFlowMap.removeEdge(edge, DependencyEdge::to)
                outgoingInformationFlowMap.removeEdge(edge, DependencyEdge::to)
            }
            is InformationEdge -> outgoingInformationFlowMap.removeEdge(edge, DependencyEdge::to)
            else -> super.removeOutgoingEdge(edge)
        }

    override fun reset() {
        super.reset()
        incomingInformationFlowMap.clear()
        outgoingInformationFlowMap.clear()
    }
}

data class CompositeNode(
    private val indices: Set<DependencyNodeIndex>,
    private val entities: SetMultimap<EnclosingEntity<*>, DependencyNodeIndex>,
    private val subgraphFlow: SetMultimap<DependencyNodeIndex, HappensBeforeEdge>,
) : DependencyNode(), Set<DependencyNodeIndex> by indices {

    val enclosingEntities: Set<EnclosingEntity<*>> get() = entities.keys

    private val incomingInformationFlowMap: MutableMap<DependencyNodeIndex, DependencyFlowMap.InformationFlowMap> = mutableMapOf()

    private val outgoingInformationFlowMap: MutableMap<DependencyNodeIndex, DependencyFlowMap.InformationFlowMap> = mutableMapOf()

    override val incomingInformationFlow: Sequence<InformationEdge> get() = asSequence().flatMap { informationFlowInto(it) }

    override val outgoingInformationFlow: Sequence<InformationEdge> get() = asSequence().flatMap { informationFlowFrom(it) }

    fun informationFlowInto(index: DependencyNodeIndex): Sequence<InformationEdge> {
        if (index !in this) return emptySequence()
        return incomingInformationFlowMap[index]?.asSequence()?.map { it.value } ?: emptySequence()
    }

    fun informationFlowFrom(index: DependencyNodeIndex): Sequence<InformationEdge> {
        if (index !in this) return emptySequence()
        return outgoingInformationFlowMap[index]?.asSequence()?.map { it.value } ?: emptySequence()
    }

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
                if (edge.from !in this) incomingHappensBeforeFlowMap.insertEdge(edge, DependencyEdge::from)
                incomingInformationFlowMap.getOrPut(edge.to) { DependencyFlowMap.InformationFlowMap() }
                    .insertEdge(edge, DependencyEdge::from)
            }
            is InformationEdge -> incomingInformationFlowMap.getOrPut(edge.to) { DependencyFlowMap.InformationFlowMap() }
                .insertEdge(edge, DependencyEdge::from)
            else -> super.insertIncomingEdge(edge)
        }

    private inline fun removeEdge(
        edge: DependencyEdge,
        happensBeforeFlowMap: DependencyFlowMap.HappensBeforeFlowMap,
        informationFlowMap: MutableMap<DependencyNodeIndex, DependencyFlowMap.InformationFlowMap>,
        containedNode: (DependencyEdge) -> DependencyNodeIndex,
        indexedNode: (DependencyEdge) -> DependencyNodeIndex,
        superCall: (DependencyEdge) -> Boolean
    ): Boolean = when (edge) {
        is IsCalledBy -> {
            if (indexedNode(edge) !in this) happensBeforeFlowMap.removeEdge(edge, indexedNode)
            val flowMap = informationFlowMap[containedNode(edge)] ?: return false
            val result = flowMap.removeEdge(edge, indexedNode)
            if (flowMap.isEmpty()) informationFlowMap.remove(containedNode(edge))
            result
        }
        is InformationEdge -> {
            val flowMap = informationFlowMap[containedNode(edge)] ?: return false
            val result = flowMap.removeEdge(edge, indexedNode)
            if (flowMap.isEmpty()) informationFlowMap.remove(containedNode(edge))
            result
        }
        else -> superCall(edge)
    }

    override fun removeIncomingEdge(edge: DependencyEdge): Boolean = removeEdge(
        edge = edge,
        happensBeforeFlowMap = incomingHappensBeforeFlowMap,
        informationFlowMap = incomingInformationFlowMap,
        containedNode = DependencyEdge::to,
        indexedNode = DependencyEdge::from,
        superCall = { super.removeIncomingEdge(it) }
    )

    override fun insertOutgoingEdge(edge: DependencyEdge): Boolean =
        when (edge) {
            is IsCalledBy -> {
                if (edge.to !in this) outgoingHappensBeforeFlowMap.insertEdge(edge, DependencyEdge::to)
                outgoingInformationFlowMap.getOrPut(edge.from) { DependencyFlowMap.InformationFlowMap() }
                    .insertEdge(edge, DependencyEdge::to)
            }
            is InformationEdge -> outgoingInformationFlowMap.getOrPut(edge.from) { DependencyFlowMap.InformationFlowMap() }
                .insertEdge(edge, DependencyEdge::to)
            else -> super.insertOutgoingEdge(edge)
        }

    override fun removeOutgoingEdge(edge: DependencyEdge): Boolean = removeEdge(
        edge = edge,
        happensBeforeFlowMap = outgoingHappensBeforeFlowMap,
        informationFlowMap = outgoingInformationFlowMap,
        containedNode = DependencyEdge::from,
        indexedNode = DependencyEdge::to,
        superCall = { super.removeOutgoingEdge(it) }
    )

    override fun reset() {
        super.reset()
        incomingInformationFlowMap.forEach { it.value.clear() }
        incomingInformationFlowMap.clear()
        outgoingInformationFlowMap.forEach { it.value.clear() }
        outgoingInformationFlowMap.clear()
    }

    companion object {
        context(cycle: CompositeNode)
        fun DependencyNodeIndex.subgraphFlowDescendants(): Sequence<DependencyNodeIndex> =
            TraversalOrder.PreOrder.traverse(
                start = this@subgraphFlowDescendants,
                predicate = { it in cycle },
                neighbours = { cycle.subgraphFlowFrom(it).map(DependencyEdge::to) }
            )

        context(cycle: CompositeNode)
        inline fun DependencyNodeIndex.cycleInformationAncestors(
            crossinline predicate: (DependencyNodeIndex) -> Boolean = { true }
        ): Sequence<DependencyNodeIndex> =
            TraversalOrder.PreOrder.traverse(
                start = this@cycleInformationAncestors,
                predicate = { it in cycle && predicate(it) },
                neighbours = { cycle.informationFlowInto(it).map(DependencyEdge::from) }
            )

        context(cycle: CompositeNode)
        inline fun DependencyNodeIndex.cycleInformationDescendants(
            crossinline predicate: (DependencyNodeIndex) -> Boolean = { true }
        ): Sequence<DependencyNodeIndex> =
            TraversalOrder.PreOrder.traverse(
                start = this@cycleInformationDescendants,
                predicate = { it in cycle && predicate(it) },
                neighbours = { cycle.informationFlowFrom(it).map(DependencyEdge::to) }
            )
    }
}
