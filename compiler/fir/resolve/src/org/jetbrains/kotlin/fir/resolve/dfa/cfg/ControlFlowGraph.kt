/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class ControlFlowGraph(val declaration: FirDeclaration?, val name: String, val kind: Kind) {
    private var _nodes: MutableList<CFGNode<*>> = mutableListOf()

    val nodes: List<CFGNode<*>>
        get() = _nodes

    internal fun addNode(node: CFGNode<*>) {
        assertState(State.Building)
        _nodes.add(node)
    }

    lateinit var enterNode: CFGNode<*>
        internal set

    lateinit var exitNode: CFGNode<*>
        internal set
    var owner: ControlFlowGraph? = null
        private set

    var state: State = State.Building
        private set

    private val _subGraphs: MutableList<ControlFlowGraph> = mutableListOf()
    val subGraphs: List<ControlFlowGraph> get() = _subGraphs

    internal fun complete() {
        assertState(State.Building)
        state = State.Completed
        if (kind == Kind.Stub) return
        val sortedNodes = orderNodes()
        // TODO Fix this
//        assert(sortedNodes.size == _nodes.size)
//        for (node in _nodes) {
//            assert(node in sortedNodes)
//        }
        _nodes.clear()
        _nodes.addAll(sortedNodes)
    }

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

    private fun assertState(state: State) {
        assert(this.state == state) {
            "This action can not be performed at $this state"
        }
    }

    enum class State {
        Building,
        Completed;
    }

    enum class Kind(val withBody: Boolean) {
        Function(withBody = true),
        AnonymousFunction(withBody = true),
        ClassInitializer(withBody = true),
        PropertyInitializer(withBody = true),
        FieldInitializer(withBody = true),
        TopLevel(withBody = false),
        AnnotationCall(withBody = true),
        DefaultArgument(withBody = false),
        Stub(withBody = true)
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

sealed class EdgeLabel(val label: String?) {
    open val isNormal: Boolean
        get() = false

    override fun toString(): String {
        return label ?: ""
    }
}

object NormalPath : EdgeLabel(label = null) {
    override val isNormal: Boolean
        get() = true
}

object LoopBackPath : EdgeLabel(label = null) {
    override val isNormal: Boolean
        get() = true
}

object UncaughtExceptionPath : EdgeLabel(label = "onUncaughtException")

// TODO: Label `return`ing edge with this.
class ReturnPath(
    returnTargetSymbol: FirFunctionSymbol<*>
) : EdgeLabel(label = "\"return@${returnTargetSymbol.callableId}\"")

enum class EdgeKind(
    val usedInDfa: Boolean,
    val usedInCfa: Boolean,
    val isBack: Boolean,
    val isDead: Boolean
) {
    Forward(usedInDfa = true, usedInCfa = true, isBack = false, isDead = false),
    DeadForward(usedInDfa = false, usedInCfa = true, isBack = false, isDead = true),
    DfgForward(usedInDfa = true, usedInCfa = false, isBack = false, isDead = false),
    CfgForward(usedInDfa = false, usedInCfa = true, isBack = false, isDead = false),
    CfgBackward(usedInDfa = false, usedInCfa = true, isBack = true, isDead = false),
    DeadBackward(usedInDfa = false, usedInCfa = true, isBack = true, isDead = true)
}

@OptIn(ExperimentalStdlibApi::class)
private fun ControlFlowGraph.orderNodes(): LinkedHashSet<CFGNode<*>> {
    val visitedNodes = linkedSetOf<CFGNode<*>>()
    /*
     * [delayedNodes] is needed to accomplish next order contract:
     *   for each node all previous node lays before it
     */
    val stack = ArrayDeque<CFGNode<*>>()
    stack.addFirst(enterNode)
    while (stack.isNotEmpty()) {
        val node = stack.removeFirst()
        val previousNodes = node.previousNodes
        if (previousNodes.any { it !in visitedNodes && it.owner == this && !node.incomingEdges.getValue(it).kind.isBack }) {
            stack.addLast(node)
            continue
        }
        if (!visitedNodes.add(node)) continue
        val followingNodes = node.followingNodes

        for (followingNode in followingNodes) {
            if (followingNode.owner == this) {
                if (followingNode !in visitedNodes) {
                    stack.addFirst(followingNode)
                }
            } else {
                walkThrowSubGraphs(followingNode.owner, visitedNodes, stack)
            }
        }
    }
    return visitedNodes
}

@OptIn(ExperimentalStdlibApi::class)
private fun ControlFlowGraph.walkThrowSubGraphs(
    otherGraph: ControlFlowGraph,
    visitedNodes: Set<CFGNode<*>>,
    stack: ArrayDeque<CFGNode<*>>
) {
    if (otherGraph.owner != this) return
    for (otherNode in otherGraph.exitNode.followingNodes) {
        if (otherNode.owner == this) {
            if (otherNode !in visitedNodes) {
                stack.addFirst(otherNode)
            }
        } else if (otherNode.owner != otherGraph) {
            walkThrowSubGraphs(otherNode.owner, visitedNodes, stack)
        }
    }
}
