/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

val CFGNode<*>.previousCfgNodes: List<CFGNode<*>>
    get() = previousNodes.filter {
        val kind = edgeFrom(it).kind
        kind.usedInCfa && (this.isDead || !kind.isDead)
    }

fun <K : Any, V : Any> ControlFlowGraph.traverseToFixedPoint(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
): Map<CFGNode<*>, PathAwareControlFlowInfo<K, V>> {
    val nodeMap = LinkedHashMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>()
    val changed = hashSetOf<CFGNode<*>>()

    // Before the first pass, the changed set will be empty.
    // After the first pass, the changed set will be full.
    // Once the changed set is empty, the traversal is stable.
    do {
        traverseOnce(visitor, nodeMap, changed)
    } while (changed.isNotEmpty())

    return nodeMap
}

private fun <K : Any, V : Any> ControlFlowGraph.traverseOnce(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>,
    changed: MutableSet<CFGNode<*>>,
    previousNodesCache: MutableMap<CFGNode<*>, List<CFGNode<*>>> = hashMapOf(),
) {
    for (node in nodes) {
        changed.remove(node)

        val previousNodes = previousNodesCache.getOrPut(node) { node.previousCfgNodes }
        if (node !in nodeMap || previousNodes.any { it in changed }) {
            var previousData: PathAwareControlFlowInfo<K, V>? = null
            for (previousNode in previousNodes) {
                // TODO(KT-59670) no reason to recalculate the edge if the node data hasn't changed.
                val nodeData = nodeMap[previousNode] ?: continue
                val edgeData = visitor.visitEdge(
                    from = previousNode,
                    to = node,
                    data = nodeData,
                    metadata = node.edgeFrom(previousNode),
                )

                previousData = when (previousData) {
                    null -> edgeData
                    else -> visitor.mergeInfo(previousData, edgeData, node)
                }
            }

            val newData = node.accept(visitor, previousData ?: emptyNormalPathInfo())
            if (newData != nodeMap.put(node, newData)) {
                changed.add(node)
            }
        }

        if (node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach {
                if (visitor.visitSubGraph(node, it)) it.traverseOnce(visitor, nodeMap, changed, previousNodesCache)
            }
        }
    }
}
