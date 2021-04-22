/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Reformat")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int, private val id: Int) {
    companion object {
        internal fun addEdge(
            from: CFGNode<*>,
            to: CFGNode<*>,
            kind: EdgeKind,
            propagateDeadness: Boolean,
            label: EdgeLabel = NormalPath
        ) {
            from._followingNodes += to
            to._previousNodes += from
            addJustKindEdge(from, to, kind, propagateDeadness, edgeExists = false, label = label)
        }

        internal fun addJustKindEdge(
            from: CFGNode<*>,
            to: CFGNode<*>,
            kind: EdgeKind,
            propagateDeadness: Boolean,
            label: EdgeLabel = NormalPath
        ) {
            addJustKindEdge(from, to, kind, propagateDeadness, edgeExists = true, label = label)
        }

        private fun addJustKindEdge(
            from: CFGNode<*>,
            to: CFGNode<*>,
            kind: EdgeKind,
            propagateDeadness: Boolean,
            edgeExists: Boolean,
            label: EdgeLabel = NormalPath
        ) {
            // It's hard to define label merging, hence overwritten with the latest one.
            // One day, if we allow multiple edges between nodes with different labels, we won't even need kind merging.
            if (kind != EdgeKind.Forward || label != NormalPath) {
                val fromToKind = from._outgoingEdges[to]?.kind ?: runIf(edgeExists) { EdgeKind.Forward }
                merge(kind, fromToKind)?.let {
                    from._outgoingEdges[to] = Edge.create(label, it)
                } ?: from._outgoingEdges.remove(to)
                val toFromKind = to._incomingEdges[from]?.kind ?: runIf(edgeExists) { EdgeKind.Forward }
                merge(kind, toFromKind)?.let {
                    to._incomingEdges[from] = Edge.create(label, it)
                } ?: to._incomingEdges.remove(from)
            }
            if (propagateDeadness && kind == EdgeKind.DeadForward) {
                to.isDead = true
            }
        }

        private fun merge(first: EdgeKind, second: EdgeKind?): EdgeKind? {
            return when {
                second == null -> first
                first == second -> first
                first == EdgeKind.DeadForward || second == EdgeKind.DeadForward -> EdgeKind.DeadForward
                first == EdgeKind.DeadBackward || second == EdgeKind.DeadBackward -> EdgeKind.DeadBackward
                first == EdgeKind.Forward || second == EdgeKind.Forward -> null
                first.usedInDfa xor second.usedInDfa -> null
                else -> throw IllegalStateException()
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

    private val _incomingEdges = mutableMapOf<CFGNode<*>, Edge>().withDefault { Edge.Normal_Forward }
    private val _outgoingEdges = mutableMapOf<CFGNode<*>, Edge>().withDefault { Edge.Normal_Forward }

    val incomingEdges: Map<CFGNode<*>, Edge> get() = _incomingEdges
    val outgoingEdges: Map<CFGNode<*>, Edge> get() = _outgoingEdges

    abstract val fir: E
    var isDead: Boolean = false
        protected set

    internal fun updateDeadStatus() {
        isDead = incomingEdges.size == previousNodes.size && incomingEdges.values.all { it.kind == EdgeKind.DeadForward }
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

// ----------------------------------- EnterNode for declaration with CFG -----------------------------------

sealed class CFGNodeWithCfgOwner<out E : FirControlFlowGraphOwner>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<E>(owner, level, id) {
    private val _subGraphs = mutableListOf<ControlFlowGraph>()

    fun addSubGraph(graph: ControlFlowGraph){
        _subGraphs += graph
    }

    val subGraphs: List<ControlFlowGraph> by lazy {
        _subGraphs.also { it.addIfNotNull(fir.controlFlowGraphReference?.controlFlowGraph) }
    }
}

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
class LocalFunctionDeclarationNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNodeWithCfgOwner<FirFunction<*>>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalFunctionDeclarationNode(this, data)
    }
}


// ----------------------------------- Default arguments -----------------------------------

class EnterDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int, id: Int) : CFGNodeWithCfgOwner<FirValueParameter>(owner, level, id), EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterDefaultArgumentsNode(this, data)
    }
}

class ExitDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int, id: Int) : CFGNode<FirValueParameter>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitDefaultArgumentsNode(this, data)
    }
}

// ----------------------------------- Anonymous function -----------------------------------

class PostponedLambdaEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNodeWithCfgOwner<FirAnonymousFunction>(owner, level, id) {
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
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassEnterNode(this, data)
    }
}

class ClassExitNode(owner: ControlFlowGraph, override val fir: FirClass<*>, level: Int, id: Int) : CFGNode<FirClass<*>>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassExitNode(this, data)
    }
}

class LocalClassExitNode(owner: ControlFlowGraph, override val fir: FirRegularClass, level: Int, id: Int) : CFGNodeWithCfgOwner<FirRegularClass>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalClassExitNode(this, data)
    }
}

class AnonymousObjectExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousObject, level: Int, id: Int) : CFGNodeWithCfgOwner<FirAnonymousObject>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectExitNode(this, data)
    }
}

// ----------------------------------- Initialization -----------------------------------

class PartOfClassInitializationNode(owner: ControlFlowGraph, override val fir: FirControlFlowGraphOwner, level: Int, id: Int) : CFGNodeWithCfgOwner<FirControlFlowGraphOwner>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPartOfClassInitializationNode(this, data)
    }
}

// ----------------------------------- Property -----------------------------------

class PropertyInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerEnterNode(this, data)
    }
}
class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerExitNode(this, data)
    }
}

// ----------------------------------- Field -----------------------------------

class FieldInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirField, level: Int, id: Int) : CFGNode<FirField>(owner, level, id), EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFieldInitializerEnterNode(this, data)
    }
}
class FieldInitializerExitNode(owner: ControlFlowGraph, override val fir: FirField, level: Int, id: Int) : CFGNode<FirField>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFieldInitializerExitNode(this, data)
    }
}

// ----------------------------------- Init -----------------------------------

class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockEnterNode(this, data)
    }
}
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

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
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, val loop: FirLoop, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), EnterNodeMarker {
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

class ComparisonExpressionNode(owner: ControlFlowGraph, override val fir: FirComparisonExpression, level: Int, id: Int) : CFGNode<FirComparisonExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitComparisonExpressionNode(this, data)
    }
}

class EqualityOperatorCallNode(owner: ControlFlowGraph, override val fir: FirEqualityOperatorCall, level: Int, id: Int) : AbstractBinaryExitNode<FirEqualityOperatorCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEqualityOperatorCallNode(this, data)
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

class CallableReferenceNode(
    owner: ControlFlowGraph,
    override val fir: FirCallableReferenceAccess,
    level: Int, id: Int
) : CFGNode<FirCallableReferenceAccess>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCallableReferenceNode(this, data)
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

class StringConcatenationCallNode(
    owner: ControlFlowGraph,
    override val fir: FirStringConcatenationCall,
    level: Int,
    id: Int
) : CFGNode<FirStringConcatenationCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitStringConcatenationCallNode(this, data)
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
    init {
        owner.enterNode = this
    }

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

// ----------------------------------- Elvis -----------------------------------

class ElvisLhsExitNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : CFGNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisLhsExitNode(this, data)
    }
}

class ElvisLhsIsNotNullNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : CFGNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisLhsIsNotNullNode(this, data)
    }
}

class ElvisRhsEnterNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : CFGNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisRhsEnterNode(this, data)
    }
}

class ElvisExitNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisExitNode(this, data)
    }
}

// ----------------------------------- Other -----------------------------------

class AnnotationEnterNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnnotationEnterNode(this, data)
    }
}
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

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
