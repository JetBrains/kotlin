/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.declarations.FirDeclaration

class ControlFlowGraph(val declaration: FirDeclaration?, val name: String, val kind: Kind) {
    @set:CfgInternals
    var nodeCount = 0

    lateinit var nodes: List<CFGNode<*>>
        private set

    @set:CfgInternals
    lateinit var enterNode: CFGNode<*>

    @set:CfgInternals
    lateinit var exitNode: CFGNode<*>

    val isSubGraph: Boolean
        get() = enterNode.previousNodes.isNotEmpty()

    val subGraphs: List<ControlFlowGraph>
        get() = nodes.flatMap { (it as? CFGNodeWithSubgraphs<*>)?.subGraphs ?: emptyList() }

    @CfgInternals
    fun complete() {
        nodes = orderNodes()
    }

    enum class Kind {
        File,
        Class,
        Constructor,
        Function,
        LocalFunction,
        AnonymousFunction,
        AnonymousFunctionCalledInPlace,
        PropertyInitializer,
        ClassInitializer,
        FieldInitializer,
        FakeCall,
        DefaultArgument,
    }

    // NOTE: this is only for dynamic dispatch on node types. If you're collecting data from predecessors,
    // use `collectDataForNode` instead to account for `finally` block deduplication. If you don't need that,
    // then you probably don't need this either. Hint: if the only thing you need from nodes is the corresponding
    // FIR structure, then traverse the `FirFile` instead.
    fun <D> traverse(visitor: ControlFlowGraphVisitor<*, D>, data: D) {
        for (node in nodes) {
            node.accept(visitor, data)
            (node as? CFGNodeWithSubgraphs<*>)?.subGraphs?.forEach { it.traverse(visitor, data) }
        }
    }

    fun traverse(visitor: ControlFlowGraphVisitorVoid) {
        traverse(visitor, null)
    }
}

data class Edge(
    val label: EdgeLabel,
    val kind: EdgeKind,
) {
    companion object {
        val Normal_Forward = Edge(NormalPath, EdgeKind.Forward)
        private val Normal_DeadForward = Edge(NormalPath, EdgeKind.DeadForward)
        private val Normal_DfgForward = Edge(NormalPath, EdgeKind.DfgForward)
        private val Normal_CfgForward = Edge(NormalPath, EdgeKind.CfgForward)
        private val Normal_CfgBackward = Edge(NormalPath, EdgeKind.CfgBackward)
        private val Normal_DeadBackward = Edge(NormalPath, EdgeKind.DeadBackward)

        fun create(label: EdgeLabel, kind: EdgeKind): Edge =
            when (label) {
                NormalPath -> {
                    when (kind) {
                        EdgeKind.Forward -> Normal_Forward
                        EdgeKind.DeadForward -> Normal_DeadForward
                        EdgeKind.DfgForward -> Normal_DfgForward
                        EdgeKind.CfgForward -> Normal_CfgForward
                        EdgeKind.CfgBackward -> Normal_CfgBackward
                        EdgeKind.DeadBackward -> Normal_DeadBackward
                    }
                }
                else -> {
                    Edge(label, kind)
                }
            }
    }
}

sealed interface EdgeLabel {
    val label: String?
}

object NormalPath : EdgeLabel {
    override val label: String? get() = null
}

object UncaughtExceptionPath : EdgeLabel {
    override val label: String get() = "onUncaughtException"
}

enum class EdgeKind(
    val usedInDfa: Boolean, // propagate flow to alive nodes
    val usedInDeadDfa: Boolean, // propagate flow to dead nodes
    val usedInCfa: Boolean,
    val isBack: Boolean,
    val isDead: Boolean
) {
    Forward(usedInDfa = true, usedInDeadDfa = true, usedInCfa = true, isBack = false, isDead = false),
    DeadForward(usedInDfa = false, usedInDeadDfa = true, usedInCfa = true, isBack = false, isDead = true),
    DfgForward(usedInDfa = true, usedInDeadDfa = true, usedInCfa = false, isBack = false, isDead = false),
    CfgForward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = false, isDead = false),
    CfgBackward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = true, isDead = false),
    DeadBackward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = true, isDead = true)
}

private val CFGNode<*>.previousNodeCount
    get() = previousNodes.count { it.owner == owner && !edgeFrom(it).kind.isBack }

private fun ControlFlowGraph.orderNodes(): List<CFGNode<*>> {
    // NOTE: this produces a BFS order. If desired, a DFS order can be created instead by using a linked list,
    // iterating over `followingNodes` in reverse order, and inserting new nodes at the current iteration point.
    val result = ArrayList<CFGNode<*>>(nodeCount).apply { add(enterNode) }
    val countdowns = IntArray(nodeCount)
    var i = 0
    while (i < result.size) {
        val node = result[i++]
        for (next in node.followingNodes) {
            if (next.owner != this) {
                // Assume nodes in this graph can be ordered in isolation. If necessary, dead edges
                // should be used to go around subgraphs that always execute.
            } else if (next.previousNodes.size == 1) {
                // Fast path: assume `next.previousNodes` is `listOf(node)`, and the edge is forward.
                // In tests, the consistency checker will validate this assumption.
                result.add(next)
            } else if (!node.edgeTo(next).kind.isBack) {
                // Can only read a 0 if never seen this node before.
                val remaining = countdowns[next.id].let { if (it == 0) next.previousNodeCount else it } - 1
                if (remaining == 0) {
                    result.add(next)
                }
                countdowns[next.id] = remaining
            }
        }
    }
    assert(result.size == nodeCount) {
        // TODO: can theoretically dump loop nodes into the output in some order so that `ControlFlowGraphRenderer`
        //  could show them for debugging purposes.
        "some nodes ${if (countdowns.all { it == 0 }) "are not reachable" else "form loops"} in control flow graph $name"
    }
    return result
}
