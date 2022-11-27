/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Reformat")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.PersistentFlow
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNothingTypeRef
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.utils.SmartList

@RequiresOptIn
annotation class CfgInternals

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int, private val id: Int) {
    companion object {
        @CfgInternals
        fun addEdge(
            from: CFGNode<*>,
            to: CFGNode<*>,
            kind: EdgeKind,
            propagateDeadness: Boolean,
            label: EdgeLabel = NormalPath
        ) {
            from._followingNodes += to
            to._previousNodes += from
            if (kind != EdgeKind.Forward || label != NormalPath) {
                to.insertIncomingEdge(from, Edge.create(label, kind))
            }
            if (propagateDeadness && kind == EdgeKind.DeadForward) {
                to.isDead = true
            }
        }

        @CfgInternals
        fun killEdge(from: CFGNode<*>, to: CFGNode<*>, propagateDeadness: Boolean): Boolean {
            val oldEdge = to.edgeFrom(from)
            if (oldEdge.kind.isDead) return false
            val newEdge = Edge.create(oldEdge.label, if (oldEdge.kind.isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward)
            to.insertIncomingEdge(from, newEdge)
            if (propagateDeadness) {
                to.isDead = true
            }
            return true
        }

        @CfgInternals
        fun removeAllIncomingEdges(to: CFGNode<*>) {
            for (from in to._previousNodes) {
                from._followingNodes.remove(to)
            }
            to._incomingEdges = null
            to._previousNodes.clear()
        }

        @CfgInternals
        fun removeAllOutgoingEdges(from: CFGNode<*>) {
            for (to in from._followingNodes) {
                to._previousNodes.remove(from)
                to._incomingEdges?.remove(from)
            }
            from._followingNodes.clear()
        }
    }

    init {
        @Suppress("LeakingThis")
        owner.addNode(this)
    }

    private val _previousNodes: MutableList<CFGNode<*>> = SmartList()
    private val _followingNodes: MutableList<CFGNode<*>> = SmartList()

    val previousNodes: List<CFGNode<*>> get() = _previousNodes
    val followingNodes: List<CFGNode<*>> get() = _followingNodes

    private var _incomingEdges: MutableMap<CFGNode<*>, Edge>? = null

    private fun insertIncomingEdge(from: CFGNode<*>, edge: Edge) {
        val map = _incomingEdges
        if (map != null) {
            map[from] = edge
        } else {
            _incomingEdges = mutableMapOf(from to edge)
        }
    }

    fun edgeFrom(other: CFGNode<*>) = _incomingEdges?.get(other) ?: Edge.Normal_Forward
    fun edgeTo(other: CFGNode<*>) = other.edgeFrom(this)

    abstract val fir: E
    var isDead: Boolean = false
        protected set

    private var _flow: PersistentFlow? = null
    open var flow: PersistentFlow
        get() = _flow ?: throw IllegalStateException("flow for $this not initialized - traversing nodes in wrong order?")
        @CfgInternals
        set(value) {
            assert(_flow == null) { "reassigning flow for $this" }
            _flow = value
        }

    @CfgInternals
    fun updateDeadStatus() {
        isDead = if (this is UnionNodeMarker)
            _incomingEdges?.let { map -> map.values.any { it.kind.isDead } } == true
        else
            _incomingEdges?.let { map -> map.size == previousNodes.size && map.values.all { it.kind.isDead || !it.kind.usedInCfa } } == true
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

//   a ---> b ---> d
//      \-> c -/
// Normal CFG semantics: a, then either b or c, then d
// If d is an instance of this interface: a, then *both* b and c in some unknown order, then d
interface UnionNodeMarker

// ----------------------------------- EnterNode for declaration with CFG -----------------------------------

sealed class CFGNodeWithSubgraphs<out E : FirElement>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<E>(owner, level, id) {
    private val _subGraphs = mutableListOf<ControlFlowGraph>()

    fun addSubGraph(graph: ControlFlowGraph){
        _subGraphs += graph
    }

    open val subGraphs: List<ControlFlowGraph>
        get() = _subGraphs
}

sealed class CFGNodeWithCfgOwner<out E : FirControlFlowGraphOwner>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNodeWithSubgraphs<E>(owner, level, id) {
    override val subGraphs: List<ControlFlowGraph> by lazy {
        fir.controlFlowGraphReference?.controlFlowGraph?.run(::addSubGraph)
        super.subGraphs
    }
}

// ----------------------------------- Named function -----------------------------------

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: FirFunction, level: Int, id: Int) : CFGNode<FirFunction>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionEnterNode(this, data)
    }
}
class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction, level: Int, id: Int) : CFGNode<FirFunction>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionExitNode(this, data)
    }
}
class LocalFunctionDeclarationNode(owner: ControlFlowGraph, override val fir: FirFunction, level: Int, id: Int) : CFGNodeWithCfgOwner<FirFunction>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalFunctionDeclarationNode(this, data)
    }
}


// ----------------------------------- Default arguments -----------------------------------

@OptIn(CfgInternals::class)
class EnterDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int, id: Int) : CFGNodeWithCfgOwner<FirValueParameter>(owner, level, id),
    EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterDefaultArgumentsNode(this, data)
    }
}

@OptIn(CfgInternals::class)
class ExitDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int, id: Int) : CFGNode<FirValueParameter>(owner, level, id),
    ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitDefaultArgumentsNode(this, data)
    }
}

// ----------------------------------- Anonymous function -----------------------------------

class SplitPostponedLambdasNode(owner: ControlFlowGraph, override val fir: FirStatement, val lambdas: List<FirAnonymousFunction>, level: Int, id: Int)
    : CFGNodeWithSubgraphs<FirStatement>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitSplitPostponedLambdasNode(this, data)
    }
}

class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunctionExpression, level: Int, id: Int) : CFGNode<FirAnonymousFunctionExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPostponedLambdaExitNode(this, data)
    }
}

class MergePostponedLambdaExitsNode(owner: ControlFlowGraph, override val fir: FirElement, level: Int, id: Int) : CFGNode<FirElement>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitMergePostponedLambdaExitsNode(this, data)
    }
}

class AnonymousFunctionExpressionNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunctionExpression, level: Int, id: Int) : CFGNodeWithSubgraphs<FirAnonymousFunctionExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousFunctionExpressionNode(this, data)
    }
}

// ----------------------------------- Classes -----------------------------------

@OptIn(CfgInternals::class)
class ClassEnterNode(owner: ControlFlowGraph, override val fir: FirClass, level: Int, id: Int) : CFGNode<FirClass>(owner, level, id),
    EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassEnterNode(this, data)
    }
}

@OptIn(CfgInternals::class)
class ClassExitNode(owner: ControlFlowGraph, override val fir: FirClass, level: Int, id: Int) : CFGNode<FirClass>(owner, level, id),
    ExitNodeMarker {
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

class AnonymousObjectEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousObject, level: Int, id: Int) : CFGNodeWithCfgOwner<FirAnonymousObject>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectEnterNode(this, data)
    }
}

class AnonymousObjectExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousObject, level: Int, id: Int) : CFGNodeWithSubgraphs<FirAnonymousObject>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectExitNode(this, data)
    }
}

class AnonymousObjectExpressionExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousObjectExpression, level: Int, id: Int) : CFGNode<FirAnonymousObjectExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectExpressionExitNode(this, data)
    }
}

// ----------------------------------- Scripts ------------------------------------------

class ScriptEnterNode(owner: ControlFlowGraph, override val fir: FirScript, level: Int, id: Int) : CFGNode<FirScript>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitScriptEnterNode(this, data)
    }
}

class ScriptExitNode(owner: ControlFlowGraph, override val fir: FirScript, level: Int, id: Int) : CFGNode<FirScript>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitScriptExitNode(this, data)
    }
}

// ----------------------------------- Initialization -----------------------------------

class PartOfClassInitializationNode(owner: ControlFlowGraph, override val fir: FirControlFlowGraphOwner, level: Int, id: Int) : CFGNodeWithCfgOwner<FirControlFlowGraphOwner>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPartOfClassInitializationNode(this, data)
    }
}

// ----------------------------------- Property -----------------------------------

@OptIn(CfgInternals::class)
class PropertyInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id),
    EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerEnterNode(this, data)
    }
}

@OptIn(CfgInternals::class)
class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id),
    ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerExitNode(this, data)
    }
}


class DelegateExpressionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int)
    : CFGNode<FirExpression>(owner, level, id), UnionNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitDelegateExpressionExitNode(this, data)
    }
}

// ----------------------------------- Field -----------------------------------

@OptIn(CfgInternals::class)
class FieldInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirField, level: Int, id: Int) : CFGNode<FirField>(owner, level, id),
    EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFieldInitializerEnterNode(this, data)
    }
}

@OptIn(CfgInternals::class)
class FieldInitializerExitNode(owner: ControlFlowGraph, override val fir: FirField, level: Int, id: Int) : CFGNode<FirField>(owner, level, id),
    ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFieldInitializerExitNode(this, data)
    }
}

// ----------------------------------- Init -----------------------------------

@OptIn(CfgInternals::class)
class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id),
    EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockEnterNode(this, data)
    }
}

@OptIn(CfgInternals::class)
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id),
    ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockExitNode(this, data)
    }
}

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockEnterNode(this, data)
    }
}
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockExitNode(this, data)
    }
}

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenEnterNode(this, data)
    }
}
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenExitNode(this, data)
    }
}
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionEnterNode(this, data)
    }
}
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id),
    ExitNodeMarker {
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
        assert(!fir.isProperlyExhaustive)
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenSyntheticElseBranchNode(this, data)
    }
}

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopEnterNode(this, data)
    }
}
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockEnterNode(this, data)
    }
}
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockExitNode(this, data)
    }
}
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, val loop: FirLoop, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionEnterNode(this, data)
    }
}
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionExitNode(this, data)
    }
}
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopExitNode(this, data)
    }
}

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionEnterNode(this, data)
    }
}
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockEnterNode(this, data)
    }
}
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockExitNode(this, data)
    }
}
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseEnterNode(this, data)
    }
}
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseExitNode(this, data)
    }
}
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockEnterNode(this, data)
    }
}
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockExitNode(this, data)
    }
}
class FinallyProxyEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyProxyEnterNode(this, data)
    }
}
class FinallyProxyExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyProxyExitNode(this, data)
    }
}
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionExitNode(this, data)
    }
}

// ----------------------------------- Boolean operators -----------------------------------

abstract class AbstractBinaryExitNode<T : FirElement>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<T>(owner, level, id) {
    val leftOperandNode: CFGNode<*> get() = previousNodes[0]
    val rightOperandNode: CFGNode<*> get() = previousNodes[1]
}

class BinaryAndEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id),
    EnterNodeMarker {
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
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitNode(this, data)
    }
}

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id),
    EnterNodeMarker {
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
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id),
    ExitNodeMarker {
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

class CheckNotNullCallNode(owner: ControlFlowGraph, override val fir: FirCheckNotNullCall, level: Int, id: Int)
    : CFGNode<FirCheckNotNullCall>(owner, level, id), UnionNodeMarker {
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

class FunctionCallNode(owner: ControlFlowGraph, override val fir: FirFunctionCall, level: Int, id: Int)
    : CFGNode<FirFunctionCall>(owner, level, id), UnionNodeMarker {
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

class GetClassCallNode(owner: ControlFlowGraph, override val fir: FirGetClassCall, level: Int, id: Int) : CFGNode<FirGetClassCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitGetClassCallNode(this, data)
    }
}

class DelegatedConstructorCallNode(owner: ControlFlowGraph, override val fir: FirDelegatedConstructorCall, level: Int, id: Int)
    : CFGNode<FirDelegatedConstructorCall>(owner, level, id), UnionNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitDelegatedConstructorCallNode(this, data)
    }
}

class StringConcatenationCallNode(owner: ControlFlowGraph, override val fir: FirStringConcatenationCall, level: Int, id: Int)
    : CFGNode<FirStringConcatenationCall>(owner, level, id), UnionNodeMarker {
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

    override var flow: PersistentFlow
        get() = firstPreviousNode.flow
        @CfgInternals
        set(_) = throw IllegalStateException("can't set flow for stub node")

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitStubNode(this, data)
    }
}

@OptIn(CfgInternals::class)
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

class EnterSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int, id: Int) : CFGNode<FirSafeCallExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterSafeCallNode(this, data)
    }
}
class ExitSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int, id: Int) : CFGNode<FirSafeCallExpression>(owner, level, id) {
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

class WhenSubjectExpressionExitNode(owner: ControlFlowGraph, override val fir: FirWhenSubjectExpression, level: Int, id: Int) : CFGNode<FirWhenSubjectExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenSubjectExpressionExitNode(this, data)
    }
}

// ----------------------------------- Other -----------------------------------

@OptIn(CfgInternals::class)
class AnnotationEnterNode(owner: ControlFlowGraph, override val fir: FirAnnotation, level: Int, id: Int) : CFGNode<FirAnnotation>(owner, level, id),
    EnterNodeMarker {
    init {
        owner.enterNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnnotationEnterNode(this, data)
    }
}

@OptIn(CfgInternals::class)
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotation, level: Int, id: Int) : CFGNode<FirAnnotation>(owner, level, id),
    ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnnotationExitNode(this, data)
    }
}

// ----------------------------------- Stub -----------------------------------

object FirStub : FirExpression() {
    override val source: KtSourceElement? get() = null
    override val typeRef: FirTypeRef = FirImplicitNothingTypeRef(null)
    override val annotations: List<FirAnnotation> get() = listOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpression = this
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) { assert(newAnnotations.isEmpty()) }
    override fun replaceTypeRef(newTypeRef: FirTypeRef) { assert(newTypeRef.isNothing) }
}

// ----------------------------------- Smart-cast node -----------------------------------

class SmartCastExpressionExitNode(owner: ControlFlowGraph, override val fir: FirSmartCastExpression, level: Int, id: Int) : CFGNode<FirSmartCastExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitSmartCastExpressionExitNode(this, data)
    }
}