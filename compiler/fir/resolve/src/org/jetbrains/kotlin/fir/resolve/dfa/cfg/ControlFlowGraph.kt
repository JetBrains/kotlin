/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Reformat")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

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

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int, private val id: Int) {
    companion object {
        internal fun addEdge(from: CFGNode<*>, to: CFGNode<*>, kind: EdgeKind, propagateDeadness: Boolean) {
            from._followingNodes += to
            to._previousNodes += from
            addJustKindEdge(from, to, kind, propagateDeadness)
        }

        private fun merge(first: EdgeKind, second: EdgeKind?): EdgeKind? {
            return when (second) {
                null, EdgeKind.Simple, first -> first
                EdgeKind.Dead -> second
                // Note: first can be only Cfg, Dfg, or Dead
                else -> when (first) {
                    EdgeKind.Cfg -> if (second == EdgeKind.Dfg) null else first
                    EdgeKind.Dfg -> if (second == EdgeKind.Cfg) null else first
                    else -> first
                }
            }
        }

        internal fun addJustKindEdge(from: CFGNode<*>, to: CFGNode<*>, kind: EdgeKind, propagateDeadness: Boolean) {
            if (kind != EdgeKind.Simple) {
                merge(kind, from._outgoingEdges[to])?.let {
                    from._outgoingEdges[to] = it
                } ?: from._outgoingEdges.remove(to)
                merge(kind, to._incomingEdges[from])?.let {
                    to._incomingEdges[from] = it
                } ?: to._incomingEdges.remove(from)
            }
            if (propagateDeadness && kind == EdgeKind.Dead) {
                to.isDead = true
            }
        }

        internal fun removeAllIncomingEdges(to: CFGNode<*>) {
            for (from in to._previousNodes) {
                from._followingNodes.remove(to)
                from._outgoingEdges.remove(to)
                to._incomingEdges.remove(from)
            }
            to._previousNodes.clear()
        }

        internal fun removeAllOutgoingEdges(from: CFGNode<*>) {
            for (to in from._followingNodes) {
                to._previousNodes.remove(from)
                from._outgoingEdges.remove(to)
                to._incomingEdges.remove(from)
            }
            from._followingNodes.clear()
        }
    }

    init {
        @Suppress("LeakingThis")
        owner.addNode(this)
    }

    private val _previousNodes: MutableList<CFGNode<*>> = mutableListOf()
    private val _followingNodes: MutableList<CFGNode<*>> = mutableListOf()

    val previousNodes: List<CFGNode<*>> get() = _previousNodes
    val followingNodes: List<CFGNode<*>> get() = _followingNodes

    private val _incomingEdges = mutableMapOf<CFGNode<*>, EdgeKind>().withDefault { EdgeKind.Simple }
    private val _outgoingEdges = mutableMapOf<CFGNode<*>, EdgeKind>().withDefault { EdgeKind.Simple }

    val incomingEdges: Map<CFGNode<*>, EdgeKind> get() = _incomingEdges
    val outgoingEdges: Map<CFGNode<*>, EdgeKind> get() = _outgoingEdges

    abstract val fir: E
    var isDead: Boolean = false
        protected set

    internal fun updateDeadStatus() {
        isDead = incomingEdges.size == previousNodes.size && incomingEdges.values.all { it == EdgeKind.Dead }
    }
    
    abstract fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R
    
    fun accept(visitor: ControlFlowGraphVisitorVoid) {
        accept(visitor, null)
    }
    
    final override fun equals(other: Any?): Boolean {
        if (other !is CFGNode<*>) return false
        return this === other
    }

    final override fun hashCode(): Int {
        return id
    }
}

val CFGNode<*>.firstPreviousNode: CFGNode<*> get() = previousNodes[0]
val CFGNode<*>.lastPreviousNode: CFGNode<*> get() = previousNodes.last()

interface EnterNodeMarker
interface ExitNodeMarker

// ----------------------------------- Named function -----------------------------------

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionEnterNode(this, data)
    }
}
class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionExitNode(this, data)
    }
}

// ----------------------------------- Anonymous function -----------------------------------

class PostponedLambdaEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNode<FirAnonymousFunction>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPostponedLambdaEnterNode(this, data)
    }
}
class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNode<FirAnonymousFunction>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPostponedLambdaExitNode(this, data)
    }
}
class UnionFunctionCallArgumentsNode(owner: ControlFlowGraph, override val fir: FirElement, level: Int, id: Int) : CFGNode<FirElement>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitUnionFunctionCallArgumentsNode(this, data)
    }
}

// ----------------------------------- Classes -----------------------------------

class ClassEnterNode(owner: ControlFlowGraph, override val fir: FirClass<*>, level: Int, id: Int) : CFGNode<FirClass<*>>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassEnterNode(this, data)
    }
}

class ClassExitNode(owner: ControlFlowGraph, override val fir: FirClass<*>, level: Int, id: Int) : CFGNode<FirClass<*>>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassExitNode(this, data)
    }
}

class LocalClassExitNode(owner: ControlFlowGraph, override val fir: FirRegularClass, level: Int, id: Int) : CFGNode<FirRegularClass>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalClassExitNode(this, data)
    }
}

class AnonymousObjectExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousObject, level: Int, id: Int) : CFGNode<FirAnonymousObject>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectExitNode(this, data)
    }
}

// ----------------------------------- Property -----------------------------------

class PropertyInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerEnterNode(this, data)
    }
}
class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerExitNode(this, data)
    }
}

// ----------------------------------- Init -----------------------------------

class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockEnterNode(this, data)
    }
}
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockExitNode(this, data)
    }
}

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockEnterNode(this, data)
    }
}
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockExitNode(this, data)
    }
}

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenEnterNode(this, data)
    }
}
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenExitNode(this, data)
    }
}
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionEnterNode(this, data)
    }
}
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionExitNode(this, data)
    }
}
class WhenBranchResultEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchResultEnterNode(this, data)
    }
}
class WhenBranchResultExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchResultExitNode(this, data)
    }
}
class WhenSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id) {
    init {
        assert(!fir.isExhaustive)
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenSyntheticElseBranchNode(this, data)
    }
}

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopEnterNode(this, data)
    }
}
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockEnterNode(this, data)
    }
}
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockExitNode(this, data)
    }
}
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionEnterNode(this, data)
    }
}
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionExitNode(this, data)
    }
}
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopExitNode(this, data)
    }
}

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionEnterNode(this, data)
    }
}
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockEnterNode(this, data)
    }
}
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockExitNode(this, data)
    }
}
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseEnterNode(this, data)
    }
}
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseExitNode(this, data)
    }
}
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockEnterNode(this, data)
    }
}
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockExitNode(this, data)
    }
}
class FinallyProxyEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyProxyEnterNode(this, data)
    }
}
class FinallyProxyExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyProxyExitNode(this, data)
    }
}
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionExitNode(this, data)
    }
}

// ----------------------------------- Boolean operators -----------------------------------

abstract class AbstractBinaryExitNode<T : FirElement>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<T>(owner, level, id) {
    val leftOperandNode: CFGNode<*> get() = previousNodes[0]
    val rightOperandNode: CFGNode<*> get() = previousNodes[1]
}

class BinaryAndEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndEnterNode(this, data)
    }
}
class BinaryAndExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitLeftOperandNode(this, data)
    }
}
class BinaryAndEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndEnterRightOperandNode(this, data)
    }
}
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitNode(this, data)
    }
}

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrEnterNode(this, data)
    }
}
class BinaryOrExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrExitLeftOperandNode(this, data)
    }
}
class BinaryOrEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrEnterRightOperandNode(this, data)
    }
}
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrExitNode(this, data)
    }
}

// ----------------------------------- Operator call -----------------------------------

class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: FirTypeOperatorCall, level: Int, id: Int) : CFGNode<FirTypeOperatorCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTypeOperatorCallNode(this, data)
    }
}
class OperatorCallNode(owner: ControlFlowGraph, override val fir: FirOperatorCall, level: Int, id: Int) : AbstractBinaryExitNode<FirOperatorCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitOperatorCallNode(this, data)
    }
}
class ComparisonExpressionNode(owner: ControlFlowGraph, override val fir: FirComparisonExpression, level: Int, id: Int) : CFGNode<FirComparisonExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitComparisonExpressionNode(this, data)
    }
}

// ----------------------------------- Jump -----------------------------------

class JumpNode(owner: ControlFlowGraph, override val fir: FirJump<*>, level: Int, id: Int) : CFGNode<FirJump<*>>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitJumpNode(this, data)
    }
}
class ConstExpressionNode(owner: ControlFlowGraph, override val fir: FirConstExpression<*>, level: Int, id: Int) : CFGNode<FirConstExpression<*>>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitConstExpressionNode(this, data)
    }
}

// ----------------------------------- Check not null call -----------------------------------

class CheckNotNullCallNode(owner: ControlFlowGraph, override val fir: FirCheckNotNullCall, level: Int, id: Int) : CFGNode<FirCheckNotNullCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCheckNotNullCallNode(this, data)
    }
}

// ----------------------------------- Resolvable call -----------------------------------

class QualifiedAccessNode(
    owner: ControlFlowGraph,
    override val fir: FirQualifiedAccessExpression,
    level: Int, id: Int
) : CFGNode<FirQualifiedAccessExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitQualifiedAccessNode(this, data)
    }
}

class ResolvedQualifierNode(
    owner: ControlFlowGraph,
    override val fir: FirResolvedQualifier,
    level: Int, id: Int
) : CFGNode<FirResolvedQualifier>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitResolvedQualifierNode(this, data)
    }
}

class FunctionCallNode(
    owner: ControlFlowGraph,
    override val fir: FirFunctionCall,
    level: Int, id: Int
) : CFGNode<FirFunctionCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionCallNode(this, data)
    }
}

class DelegatedConstructorCallNode(
    owner: ControlFlowGraph,
    override val fir: FirDelegatedConstructorCall,
    level: Int,
    id: Int
) : CFGNode<FirDelegatedConstructorCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitDelegatedConstructorCallNode(this, data)
    }
}

class ThrowExceptionNode(
    owner: ControlFlowGraph,
    override val fir: FirThrowExpression,
    level: Int, id: Int
) : CFGNode<FirThrowExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitThrowExceptionNode(this, data)
    }
}

class StubNode(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<FirStub>(owner, level, id) {
    init {
        isDead = true
    }

    override val fir: FirStub get() = FirStub

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitStubNode(this, data)
    }
}

class ContractDescriptionEnterNode(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<FirStub>(owner, level, id) {
    override val fir: FirStub = FirStub
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitContractDescriptionEnterNode(this, data)
    }
}

class VariableDeclarationNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitVariableDeclarationNode(this, data)
    }
}
class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: FirVariableAssignment, level: Int, id: Int) : CFGNode<FirVariableAssignment>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitVariableAssignmentNode(this, data)
    }
}

class EnterContractNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterContractNode(this, data)
    }
}
class ExitContractNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitContractNode(this, data)
    }
}

class EnterSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int, id: Int) : CFGNode<FirSafeCallExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterSafeCallNode(this, data)
    }
}
class ExitSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int, id: Int) : CFGNode<FirSafeCallExpression>(owner, level, id) {
    val lastPreviousNode: CFGNode<*> get() = previousNodes.last()
    val secondPreviousNode: CFGNode<*>? get() = previousNodes.getOrNull(1)

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitSafeCallNode(this, data)
    }
}

// ----------------------------------- Other -----------------------------------

class AnnotationEnterNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnnotationEnterNode(this, data)
    }
}
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnnotationExitNode(this, data)
    }
}

// ----------------------------------- Stub -----------------------------------

object FirStub : FirElement {
    override val source: FirSourceElement? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}
