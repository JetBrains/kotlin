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
): Map<CFGNode<*>, PathAwareControlFlowInfo<I>> {
    val nodeMap = LinkedHashMap<CFGNode<*>, PathAwareControlFlowInfo<I>>()
    var shouldContinue: Boolean
    do {
        shouldContinue = collectDataForNodeInternal(direction, visitor, nodeMap)
    } while (shouldContinue)
    return nodeMap
}

private fun <I : ControlFlowInfo<I, *, *>> ControlFlowGraph.collectDataForNodeInternal(
    direction: TraverseDirection,
    visitor: PathAwareControlFlowGraphVisitor<I>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<I>>,
): Boolean {
    var changed = false
    for (node in getNodesInOrder(direction)) {
        if (direction == TraverseDirection.Backward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach {
                changed = changed or (visitor.visitSubGraph(node, it) && it.collectDataForNodeInternal(direction, visitor, nodeMap))
            }
        }
        // TODO, KT-59670: if data for previousNodes hasn't changed, then should be no need to recompute data for this one
        val union = node.isUnion
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
        }.reduceOrNull { a, b -> a.join(b, union) }
        val newData = node.accept(visitor, previousData ?: visitor.emptyInfo)
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
