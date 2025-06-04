/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.Edge

val CFGNode<*>.previousCfgNodes: List<CFGNode<*>>
    get() = previousNodes.filter {
        val kind = edgeFrom(it).kind
        kind.usedInCfa && (this.isDead || !kind.isDead)
    }

val CFGNode<*>.followingCfgNodes: List<CFGNode<*>>
    get() = followingNodes.filter {
        val kind = it.edgeFrom(this).kind
        kind.usedInCfa && (it.isDead || !kind.isDead)
    }

enum class CfgTraverseDirection {
    Forward,
    Backward,
    ;

    fun nodes(graph: ControlFlowGraph): List<CFGNode<*>> {
        return when (this) {
            Forward -> graph.nodes
            Backward -> graph.nodes.asReversed()
        }
    }

    fun next(node: CFGNode<*>): List<CFGNode<*>> {
        return when (this) {
            Forward -> node.followingNodes
            Backward -> node.previousNodes
        }
    }

    fun previous(node: CFGNode<*>): List<CFGNode<*>> {
        return when (this) {
            Forward -> node.previousNodes
            Backward -> node.followingNodes
        }
    }

    fun edge(previous: CFGNode<*>, next: CFGNode<*>): Edge {
        return when (this) {
            Forward -> next.edgeFrom(previous)
            Backward -> previous.edgeFrom(next)
        }
    }

    fun isUsedInCfg(previous: CFGNode<*>, next: CFGNode<*>, edge: Edge): Boolean {
        val kind = edge.kind
        return kind.usedInCfa && (!kind.isDead || when (this) {
            Forward -> next.isDead
            Backward -> previous.isDead
        })
    }
}

fun <K : Any, V : Any> ControlFlowGraph.traverseToFixedPoint(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
): Map<CFGNode<*>, PathAwareControlFlowInfo<K, V>> {
    val nodeMap = LinkedHashMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>()
    while (traverseOnce(visitor, nodeMap)) {
        // had changes, continue
    }
    return nodeMap
}

private fun <K : Any, V : Any> ControlFlowGraph.traverseOnce(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>,
): Boolean {
    val direction = visitor.direction

    var changed = false
    for (node in direction.nodes(this)) {
        // TODO, KT-59670: if data for previousNodes hasn't changed, then should be no need to recompute data for this one
        val previousNodes = direction.previous(node)
            .filter { direction.isUsedInCfg(it, node, direction.edge(it, node)) }
        val previousData = previousNodes.mapNotNull { edgeData(visitor, it, node, nodeMap) }
        val previousDatum = previousData.reduceOrNull { a, b -> visitor.mergeInfo(a, b, node) }
        val newDatum = node.accept(visitor, previousDatum ?: emptyNormalPathInfo())
        if (newDatum != nodeMap.put(node, newDatum)) {
            changed = true
        }
        if (node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach {
                changed = changed or (visitor.visitSubGraph(node, it) && it.traverseOnce(visitor, nodeMap))
            }
        }
    }
    return changed
}

private fun <K : Any, V : Any> edgeData(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    source: CFGNode<*>,
    node: CFGNode<*>,
    nodeMap: Map<CFGNode<*>, PathAwareControlFlowInfo<K, V>>,
): PathAwareControlFlowInfo<K, V>? {
    val direction = visitor.direction
    val edge = direction.edge(source, node)
    if (!direction.isUsedInCfg(source, node, edge)) return null
    val data = nodeMap[source] ?: return null
    return visitor.visitEdge(source, node, edge, data)
}
