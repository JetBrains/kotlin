/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

enum class TraverseDirection {
    Forward, Backward
}

fun <I : ControlFlowInfo<I, *, *>> ControlFlowGraph.collectDataForNode(
    direction: TraverseDirection,
    visitor: PathAwareControlFlowGraphVisitor<I>,
    visitSubGraphs: Boolean = true
): Map<CFGNode<*>, PathAwareControlFlowInfo<I>> {
    val nodeMap = LinkedHashMap<CFGNode<*>, PathAwareControlFlowInfo<I>>()
    val startNode = getEnterNode(direction)
    nodeMap[startNode] = visitor.emptyInfo

    var shouldContinue: Boolean
    do {
        shouldContinue = collectDataForNodeInternal(direction, visitor, nodeMap, visitSubGraphs)
    } while (shouldContinue)

    return nodeMap
}

private fun <I : ControlFlowInfo<I, *, *>> ControlFlowGraph.collectDataForNodeInternal(
    direction: TraverseDirection,
    visitor: PathAwareControlFlowGraphVisitor<I>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<I>>,
    visitSubGraphs: Boolean = true
): Boolean {
    var changed = false
    val nodes = getNodesInOrder(direction)
    for (node in nodes) {
        if (visitSubGraphs && direction == TraverseDirection.Backward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach { changed = changed or it.collectDataForNodeInternal(direction, visitor, nodeMap) }
        }
        val previousNodes = when (direction) {
            TraverseDirection.Forward -> node.previousCfgNodes
            TraverseDirection.Backward -> node.followingCfgNodes
        }
        // TODO: if data for previousNodes hasn't changed, then should be no need to recompute data for this one
        val union = node is UnionNodeMarker
        val previousData =
            previousNodes.mapNotNull {
                val k = when (direction) {
                    TraverseDirection.Forward -> node.edgeFrom(it)
                    TraverseDirection.Backward -> node.edgeTo(it)
                }
                val v = nodeMap[it] ?: return@mapNotNull null
                visitor.visitEdge(it, node, k, v)
            }.reduceOrNull { a, b -> a.join(b, union) }
        val data = nodeMap[node]
        val newData = node.accept(visitor, previousData ?: visitor.emptyInfo)
        val hasChanged = newData != data
        changed = changed or hasChanged
        if (hasChanged) {
            nodeMap[node] = newData
        }
        if (visitSubGraphs && direction == TraverseDirection.Forward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach { changed = changed or it.collectDataForNodeInternal(direction, visitor, nodeMap) }
        }
    }
    return changed
}
