/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class ControlFlowGraph(val declaration: FirDeclaration?, val name: String, val kind: Kind) {
    private var _nodes: MutableList<CFGNode<*>> = mutableListOf()

    val nodes: List<CFGNode<*>>
        get() = _nodes

    internal fun addNode(node: CFGNode<*>) {
        assertState(State.Building)
        _nodes.add(node)
    }

    @set:CfgInternals
    lateinit var enterNode: CFGNode<*>

    @set:CfgInternals
    lateinit var exitNode: CFGNode<*>

    val isSubGraph: Boolean
        get() = enterNode.previousNodes.isNotEmpty()

    var state: State = State.Building
        private set

    val subGraphs: List<ControlFlowGraph>
        get() = _nodes.flatMap { (it as? CFGNodeWithSubgraphs<*>)?.subGraphs ?: emptyList() }

    @CfgInternals
    fun complete() {
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
        ClassInitializer(withBody = false),
        PropertyInitializer(withBody = true),
        FieldInitializer(withBody = true),
        TopLevel(withBody = false),
        FakeCall(withBody = true),
        DefaultArgument(withBody = true),
        Stub(withBody = false)
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
data class ReturnPath(val target: FirFunctionSymbol<*>) : EdgeLabel(label = "return@${target.callableId}")
data class LoopBreakPath(val loop: FirLoop) : EdgeLabel(loop.label?.let { "break@${it.name}" } ?: "break")
data class LoopContinuePath(val loop: FirLoop) : EdgeLabel(loop.label?.let { "continue@${it.name}" } ?: "continue")

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
        if (previousNodes.any { it !in visitedNodes && it.owner == this && !node.edgeFrom(it).kind.isBack }) {
            stack.addLast(node)
            continue
        }
        if (!visitedNodes.add(node)) continue

        // NOTE: this intentionally does not walk through subgraphs. If the only path from A to B
        //  is through a subgraph, add a dead edge between them to enforce ordering.
        for (followingNode in node.followingNodes) {
            if (followingNode.owner == this) {
                if (followingNode !in visitedNodes) {
                    stack.addFirst(followingNode)
                }
            }
        }
    }
    return visitedNodes
}
