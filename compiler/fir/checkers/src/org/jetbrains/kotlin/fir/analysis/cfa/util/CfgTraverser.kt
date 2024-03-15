/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

enum class TraverseDirection {
    Forward, Backward
}

fun <K : Any, V : Any> ControlFlowGraph.collectDataForNode(
    direction: TraverseDirection,
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
): Map<CFGNode<*>, PathAwareControlFlowInfo<K, V>> {
    val nodeMap = LinkedHashMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>()
    var shouldContinue: Boolean
    do {
        shouldContinue = collectDataForNodeInternal(direction, visitor, nodeMap)
    } while (shouldContinue)
    return nodeMap
}

private fun <K : Any, V : Any> ControlFlowGraph.collectDataForNodeInternal(
    direction: TraverseDirection,
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>,
): Boolean {
    var changed = false
    for (node in getNodesInOrder(direction)) {
        if (direction == TraverseDirection.Backward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach {
                changed = changed or (visitor.visitSubGraph(node, it) && it.collectDataForNodeInternal(direction, visitor, nodeMap))
            }
        }
        // TODO, KT-59670: if data for previousNodes hasn't changed, then should be no need to recompute data for this one
        val previousData = when (direction) {
            TraverseDirection.Forward -> node.previousCfgNodes
            TraverseDirection.Backward -> node.followingCfgNodes
        }.mapNotNull { source ->
            nodeMap[source]?.let {
                val edge = when (direction) {
                    TraverseDirection.Forward -> node.edgeFrom(source)
                    TraverseDirection.Backward -> node.edgeTo(source)
                }
                visitor.visitEdge(source, node, edge, it)
            }
        }.reduceOrNull { a, b -> visitor.mergeInfo(a, b, node) }
        val newData = node.accept(visitor, previousData ?: emptyNormalPathInfo())
        if (newData != nodeMap.put(node, newData)) {
            changed = true
        }
        if (direction == TraverseDirection.Forward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach {
                changed = changed or (visitor.visitSubGraph(node, it) && it.collectDataForNodeInternal(direction, visitor, nodeMap))
            }
        }
    }
    return changed
}
