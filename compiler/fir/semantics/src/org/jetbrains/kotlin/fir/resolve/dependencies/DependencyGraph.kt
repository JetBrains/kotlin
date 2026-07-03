/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNode.Companion.happensBeforeAncestors
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNode.Companion.happensBeforeDescendants
import org.jetbrains.kotlin.fir.resolve.dfa.isNotEmpty
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import java.util.LinkedList
import kotlin.collections.plusAssign
import kotlin.sequences.forEach

class DependencyGraph(val session: FirSession) : FirSessionComponent, Set<DependencyNode> {

    private val nodes: MutableSet<DependencyNode> = mutableSetOf()
    private val entities: SetMultimap<EnclosingEntity<*>, DependencyNodeIndex> = setMultimapOf()
    private val indices: MutableMap<DependencyNodeIndex, DependencyNode> = mutableMapOf()

    override val size: Int get() = nodes.size

    override fun isEmpty(): Boolean = nodes.isEmpty()

    override fun contains(element: DependencyNode): Boolean = element.index in this

    override fun iterator(): Iterator<DependencyNode> = nodes.iterator()

    override fun containsAll(elements: Collection<DependencyNode>): Boolean = elements.all { it in this }

    operator fun get(index: DependencyNodeIndex): DependencyNode? = indices[index]

    operator fun get(enclosingEntity: EnclosingEntity<*>): Sequence<DependencyNodeIndex> = entities[enclosingEntity].asSequence()

    internal inline fun getOrCreate(index: DependencyNodeIndex, init: (UnitNode) -> Unit = {}): DependencyNode =
        this[index] ?: UnitNode(index).apply {
            nodes.add(this)
            indices[index] = this
            enclosingEntity?.let { entities.put(it, index) }
            init(this)
        }

    operator fun contains(index: DependencyNodeIndex): Boolean = index in indices

    operator fun contains(enclosingEntity: EnclosingEntity<*>): Boolean = enclosingEntity in entities

    companion object {

        context(graph: DependencyGraph)
        fun Set<DependencyNode>.stronglyConnectedComponents(): List<Set<DependencyNode>> {
            val visited = mutableSetOf<DependencyNode>()
            val sorted = stackOf<DependencyNode>()
            this@stronglyConnectedComponents.forEach { node ->
                node.happensBeforeDescendants(visited, TraversalOrder.PostOrder) { it in this && !it.isComposite }
                    .forEach(sorted::push)
            }
            visited.clear()

            val result = LinkedList<Set<DependencyNode>>()
            while (sorted.isNotEmpty) {
                val current = sorted.pop()
                if (current !in visited) {
                    val component = mutableSetOf<DependencyNode>()
                    current.happensBeforeAncestors(visited, TraversalOrder.PostOrder) { it in this && !it.isComposite }
                        .forEach { component += it }
                    result += component
                }
            }

            return result
        }

        context(graph: DependencyGraph)
        fun Set<DependencyNode>.condenseCycles(): Unit =
            this@condenseCycles.stronglyConnectedComponents().forEach { component ->
                if (component.size == 1) return@forEach
                val condensed = CompositeNode(
                    // Preserve the flat structure of SCCs
                    indices = component.flatMapTo(linkedSetOf()) {
                        when (it) {
                            is UnitNode -> setOf(it.index)
                            is CompositeNode -> it.index.indices
                        }
                    },
                    entities = setMultimapOf<EnclosingEntity<*>, DependencyNodeIndex>().apply {
                        component.forEach { node ->
                            when (node) {
                                is UnitNode -> node.enclosingEntity?.let { put(it, node.index) }
                                is CompositeNode -> node.enclosingEntities.forEach { entity ->
                                    node[entity].forEach { put(entity, it) }
                                }
                            }
                        }
                    },
                    subgraphFlow = setMultimapOf<DependencyNodeIndex, HappensBeforeEdge>().apply {
                        component.forEach { node ->
                            when (node) {
                                is UnitNode -> node.happensAfterFlow.filter { it.holdsInAllExecutions }.forEach {
                                    put(node.index, it)
                                }
                                is CompositeNode -> node.index.indices.forEach { index ->
                                    node.subgraphFlowFrom(index).forEach { put(index, it) }
                                    node.happensAfterFlow.filter { it.holdsInAllExecutions }.forEach { edge ->
                                        put(index, MustHappenBefore(index, edge.to))
                                    }
                                }
                            }
                        }
                    }
                )
                component.mergeFlow(condensed)
            }

        context(graph: DependencyGraph)
        private fun Set<DependencyNode>.mergeFlow(into: CompositeNode) = let { scc ->
            val index = into.index
            // Add the node to the graph
            graph.nodes.add(into)
            // Store this to allow lookups of composite nodes along happens-before paths
            graph.indices[index] = into
            // For each node in the set that was condensed, ...
            scc.forEach { node ->
                // For each incoming happens-before edge, ...
                node.happensBeforeFlow.forEach { edge ->
                    when (edge) {
                        // Insert the call edge to the new condensed node (implicitly handles self-loops)
                        is IsCalledBy -> into.insertIncomingEdge(edge)
                        // Merge all incoming happens-before dependencies into the new condensed node and update their targets
                        is MustHappenBefore -> {
                            // Skip edges which connect nodes in the SCC
                            if (edge.from in into) return@forEach
                            val newEdge = MustHappenBefore(edge.from, index)
                            into.insertIncomingEdge(newEdge)
                            graph[edge.from]?.let { from ->
                                from.removeOutgoingEdge(edge)
                                from.insertOutgoingEdge(newEdge)
                            }
                        }
                        is MayHappenBefore -> {
                            // Skip edges which connect nodes in the SCC
                            if (edge.from in into) return@forEach
                            val newEdge = MayHappenBefore(edge.from, index)
                            into.insertIncomingEdge(newEdge)
                            graph[edge.from]?.let { from ->
                                from.removeOutgoingEdge(edge)
                                from.insertOutgoingEdge(newEdge)
                            }
                        }
                    }
                }
                // For each outgoing happens-before edge,
                node.happensAfterFlow.forEach { edge ->
                    when (edge) {
                        // Insert the call edge to the new condensed node (implicitly handles self-loops)
                        is IsCalledBy -> into.insertOutgoingEdge(edge)
                        // Merge all incoming happens-before dependencies into the new condensed node and update their targets
                        is MustHappenBefore -> {
                            // Skip edges which connect nodes in the SCC
                            if (edge.to in into) return@forEach
                            val newEdge = MustHappenBefore(index, edge.to)
                            into.insertOutgoingEdge(newEdge)
                            graph[edge.to]?.let { to ->
                                to.removeIncomingEdge(edge)
                                to.insertIncomingEdge(newEdge)
                            }
                        }
                        is MayHappenBefore -> {
                            // Skip edges which connect nodes in the SCC
                            if (edge.to in into) return@forEach
                            val newEdge = MayHappenBefore(index, edge.to)
                            into.insertOutgoingEdge(newEdge)
                            graph[edge.to]?.let { to ->
                                to.removeIncomingEdge(edge)
                                to.insertIncomingEdge(newEdge)
                            }
                        }
                    }
                }
                // Update the graph's indices, and for each (incoming) information edge, merge it into the new condensed node
                when (node) {
                    is UnitNode -> {
                        // For unit nodes, we keep the mapping of their node indices to this condensed node,
                        // as we require their presence in the graph for further analysis of their accesses
                        graph.indices[node.index] = into
                        node.informationFlow.forEach { into.insertIncomingEdge(it) }
                    }
                    is CompositeNode -> {
                        // For composite nodes, they are only preserved through time dependencies, so once the node
                        // is detached, it has no accesses by itself and can be safely removed from the graph
                        graph.indices.remove(node.index)
                        node.asSequence().flatMap { node.informationFlowInto(it) }
                            .forEach { into.insertIncomingEdge(it) }
                    }
                }
                // Dispose of the node
                node.reset()
                graph.nodes.remove(node)
            }
        }
    }
}

val FirSession.dependencyGraph: DependencyGraph? by FirSession.nullableSessionComponentAccessor()
