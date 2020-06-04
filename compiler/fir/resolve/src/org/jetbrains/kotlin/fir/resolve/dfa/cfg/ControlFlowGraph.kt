/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Reformat")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.declarations.FirDeclaration

class ControlFlowGraph(val declaration: FirDeclaration?, val name: String, val kind: Kind) {
    private val _nodes: MutableList<CFGNode<*>> = mutableListOf()

    val nodes: List<CFGNode<*>> get() = _nodes

    internal fun addNode(node: CFGNode<*>) {
        _nodes += node
    }

    lateinit var enterNode: CFGNode<*>
        internal set
    lateinit var exitNode: CFGNode<*>
        internal set

    var owner: ControlFlowGraph? = null
        private set

    private val _subGraphs: MutableList<ControlFlowGraph> = mutableListOf()
    val subGraphs: List<ControlFlowGraph> get() = _subGraphs

    internal fun addSubGraph(graph: ControlFlowGraph) {
        assert(graph.owner == null) {
            "SubGraph already has owner"
        }
        graph.owner = this
        _subGraphs += graph
    }

    internal fun removeSubGraph(graph: ControlFlowGraph) {
        assert(graph.owner == this)
        _subGraphs.remove(graph)
        graph.owner = null

        CFGNode.removeAllIncomingEdges(graph.enterNode)
        CFGNode.removeAllOutgoingEdges(graph.exitNode)
    }

    enum class Kind {
        Function, ClassInitializer, TopLevel
    }
}

enum class EdgeKind(val usedInDfa: Boolean) {
    Simple(usedInDfa = true),
    Dead(usedInDfa = false),
    Cfg(usedInDfa = false),
    Dfg(usedInDfa = true)
}
