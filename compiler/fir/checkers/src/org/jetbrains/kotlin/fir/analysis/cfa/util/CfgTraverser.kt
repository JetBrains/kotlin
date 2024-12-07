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
    while (traverseOnce(visitor, nodeMap)) {
        // had changes, continue
    }
    return nodeMap
}

private fun <K : Any, V : Any> ControlFlowGraph.traverseOnce(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>,
): Boolean {
    var changed = false
    for (node in nodes) {
        // TODO, KT-59670: if data for previousNodes hasn't changed, then should be no need to recompute data for this one
        val previousData = node.previousCfgNodes.mapNotNull { source ->
            nodeMap[source]?.let {
                visitor.visitEdge(source, node, node.edgeFrom(source), it)
            }
        }.reduceOrNull { a, b -> visitor.mergeInfo(a, b, node) }
        val newData = node.accept(visitor, previousData ?: emptyNormalPathInfo())
        if (newData != nodeMap.put(node, newData)) {
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
