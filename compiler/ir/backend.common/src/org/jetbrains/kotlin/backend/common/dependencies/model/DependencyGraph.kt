/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies

import org.jetbrains.kotlin.backend.common.dependencies.DependencyNode.Companion.happensBeforeAncestors
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNode.Companion.happensBeforeDescendants
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.util.SetMultimap
import org.jetbrains.kotlin.backend.common.dependencies.util.TraversalOrder
import org.jetbrains.kotlin.backend.common.dependencies.util.setMultimapOf
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.util.LinkedList
import kotlin.collections.plusAssign
import kotlin.sequences.forEach

class DependencyGraph(val module: IrModuleFragment) : Set<DependencyNode> {

    private val nodes: MutableSet<DependencyNode> = mutableSetOf()
    private val entities: SetMultimap<EnclosingEntity<*>, DependencyNodeIndex> = setMultimapOf()
    private val indices: MutableMap<DependencyNodeIndex, DependencyNode> = mutableMapOf()

    override val size: Int get() = nodes.size

    override fun isEmpty(): Boolean = nodes.isEmpty()

    override fun contains(element: DependencyNode): Boolean = element.index in this

    override fun iterator(): Iterator<DependencyNode> = nodes.iterator()

    override fun containsAll(elements: Collection<DependencyNode>): Boolean = elements.all { it in this }

    operator fun get(index: DependencyNodeIndex): DependencyNode? = index.unwrap().firstNotNullOfOrNull { indices[it] }

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
            val sorted = LinkedList<DependencyNode>()
            this@stronglyConnectedComponents.forEach { node ->
                node.happensBeforeDescendants(visited, TraversalOrder.PostOrder) { it in this && !it.isComposite }
                    .forEach(sorted::push)
            }
            visited.clear()

            val result = LinkedList<Set<DependencyNode>>()
            while (sorted.isNotEmpty()) {
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
                // Preserve the flat structure of SCCs
                val indices = sequence {
                    component.forEach {
                        yieldAll(it.index.unwrap())
                    }
                }.toSet()
                val condensed = CompositeNode(
                    indices = indices,
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
                    // Edges between nodes inside SCCs should have only one source and target index
                    subgraphFlow = setMultimapOf<DependencyNodeIndex, HappensBeforeEdge>().apply {
                        component.forEach { node ->
                            when (node) {
                                is UnitNode -> node.happensAfterFlow.filter { it.holdsInAllExecutions }.forEach { edge ->
                                    edge.to.unwrap().forEach { target ->
                                        put(node.index, MustHappenBefore(node.index, target))
                                    }
                                }
                                is CompositeNode -> node.index.unwrap().forEach { index ->
                                    // Invariant: all subgraph edges have a single source and target index
                                    node.subgraphFlowFrom(index).forEach { put(index, it) }
                                    node.happensAfterFlow.filter { it.holdsInAllExecutions }.forEach { edge ->
                                        val sources = edge.from.unwrap()
                                        val targets = edge.to.unwrap()
                                        sources.forEach { source ->
                                            targets.forEach { target ->
                                                put(source, MustHappenBefore(source, target))
                                            }
                                        }
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
            index.indices.forEach { graph.indices[it] = into }
            // Edges coming INTO the SCC should be merged if their source is the same, such that the target indices are exactly those
            // that were directly connected to the source node with the original edge(s)
            val incomingMergedMustFlow = mutableMapOf<DependencyNodeIndex, MustHappenBefore>()
            val incomingMergedMayFlow = mutableMapOf<DependencyNodeIndex, MayHappenBefore>()
            // Edges coming OUT of the SCC should be merged if their target is the same, such that the source indices are exactly those
            // that were directly connected to the target node with the original edge(s)
            val outgoingMergedMustFlow = mutableMapOf<DependencyNodeIndex, MustHappenBefore>()
            val outgoingMergedMayFlow = mutableMapOf<DependencyNodeIndex, MayHappenBefore>()
            // IMPORTANT: the equivalent back edges for the nodes inside the SCC that point outside the SCC should be kept, so the
            // must-happens-before subgraphs are properly preserved
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
                            incomingMergedMustFlow[edge.from] = incomingMergedMustFlow[edge.from]?.merge(edge) ?: edge
                            graph[edge.from]?.removeOutgoingEdge(edge)
                        }
                        is MayHappenBefore -> {
                            // Skip edges which connect nodes in the SCC
                            if (edge.from in into) return@forEach
                            // No need to merge, as we try to minimize the amount of may-happen-before edges
                            incomingMergedMayFlow[edge.from] = MayHappenBefore(edge.from, index)
                            graph[edge.from]?.removeOutgoingEdge(edge)
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
                            outgoingMergedMustFlow[edge.to] = outgoingMergedMustFlow[edge.to]?.merge(edge) ?: edge
                            graph[edge.from]?.removeOutgoingEdge(edge)
                        }
                        is MayHappenBefore -> {
                            // Skip edges which connect nodes in the SCC
                            if (edge.to in into) return@forEach
                            val actualTo = graph[edge.to]?.index ?: edge.to
                            // No need to merge, as we try to minimize the amount of may-happen-before edges
                            outgoingMergedMayFlow[actualTo] = MayHappenBefore(index, edge.to)
                            graph[edge.from]?.removeOutgoingEdge(edge)
                        }
                    }
                }
                // For each information edges, simply insert them to the condensed node
                node.informationFlow.forEach { into.insertIncomingEdge(it) }
                // For each merged edge, also insert them to the condensed node and the source/target nodes
                incomingMergedMustFlow.forEach { [index, edge] ->
                    into.insertIncomingEdge(edge)
                    graph[index]?.insertOutgoingEdge(edge)
                }
                incomingMergedMayFlow.forEach { [index, edge] ->
                    into.insertIncomingEdge(edge)
                    graph[index]?.insertOutgoingEdge(edge)
                }
                outgoingMergedMustFlow.forEach { [index, edge] ->
                    into.insertOutgoingEdge(edge)
                    graph[index]?.insertIncomingEdge(edge)
                }
                outgoingMergedMayFlow.forEach { [index, edge] ->
                    into.insertOutgoingEdge(edge)
                    graph[index]?.insertIncomingEdge(edge)
                }
                // Composite nodes need to be removed from the dependency graph index
                if (node.isComposite) graph.indices.remove(node.index)
                // Dispose of the node
                node.reset()
                graph.nodes.remove(node)
            }
        }
    }
}
