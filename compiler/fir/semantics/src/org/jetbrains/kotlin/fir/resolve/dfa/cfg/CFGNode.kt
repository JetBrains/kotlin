/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Reformat")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.FlowPath
import org.jetbrains.kotlin.fir.resolve.dfa.PersistentFlow
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.SmartList

@RequiresOptIn
annotation class CfgInternals

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int) {
    @OptIn(CfgInternals::class)
    val id = owner.nodeCount++

    //   a ---> b ---> d
    //      \-> c -/
    // Normal CFG semantics: a, then either b or c, then d
    // If d is a union node: a, then *both* b and c in some unknown order, then d
    open val isUnion: Boolean get() = false

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
        fun removeAllOutgoingEdges(from: CFGNode<*>) {
            for (to in from._followingNodes) {
                to._previousNodes.remove(from)
                to._incomingEdges?.remove(from)
            }
            from._followingNodes.clear()
        }

        @CfgInternals
        fun removeAllIncomingEdges(to: CFGNode<*>) {
            for (from in to._previousNodes) {
                from._followingNodes.remove(to)
            }
            to._previousNodes.clear()
            to._incomingEdges?.clear()
        }
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

    /**
     * [Flow][org.jetbrains.kotlin.fir.resolve.dfa.Flow] representing the [default path][FlowPath.Default] for this node. This flow should
     * be used for all type resolutions at this node.
     */
    private var _flow: PersistentFlow? = null
    open var flow: PersistentFlow
        get() = _flow ?: throw IllegalStateException("flow for $this not initialized - traversing nodes in wrong order?")
        @CfgInternals
        set(value) {
            assert(_flow == null) { "reassigning flow for $this" }
            _flow = value
        }

    /**
     * All other [flows][org.jetbrains.kotlin.fir.resolve.dfa.Flow] through this node which are not the [default][FlowPath.Default]. These
     * flows should only be used by following nodes when the path through this node diverges (ex., following a `finally` code block).
     */
    private var _alternateFlows: MutableMap<FlowPath, PersistentFlow>? = null
    open val alternateFlowPaths: Set<FlowPath>
        get() = _alternateFlows?.keys ?: emptySet()

    open fun getAlternateFlow(path: FlowPath): PersistentFlow? {
        return _alternateFlows?.get(path)
    }

    @CfgInternals
    open fun addAlternateFlow(path: FlowPath, flow: PersistentFlow) {
        assert(path !== FlowPath.Default) { "cannot add default flow path as alternate for $this" }
        assert(_alternateFlows?.get(path) == null) { "reassigning $path flow for $this" }

        var alternateFlows = _alternateFlows
        if (alternateFlows == null) {
            alternateFlows = mutableMapOf()
            _alternateFlows = alternateFlows
        }

        alternateFlows[path] = flow
    }

    @CfgInternals
    fun updateDeadStatus() {
        isDead = if (isUnion)
            _incomingEdges?.let { map -> map.values.any { it.kind.isDead } } == true
        else
            _incomingEdges?.let { map -> map.size == previousNodes.size && map.values.all { it.kind.isDead || !it.kind.usedInCfa } } == true
    }

    abstract fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R

    fun accept(visitor: ControlFlowGraphVisitorVoid) {
        accept(visitor, null)
    }
}

val CFGNode<*>.firstPreviousNode: CFGNode<*> get() = previousNodes[0]
val CFGNode<*>.lastPreviousNode: CFGNode<*> get() = previousNodes.last()
fun CFGNode<*>.usedInDfa(edge: Edge) = if (isDead) edge.kind.usedInDeadDfa else edge.kind.usedInDfa
val CFGNode<*>.previousLiveNodes: List<CFGNode<*>>
    get() = when  {
        this.isDead -> previousNodes
        else -> previousNodes.filter { !it.isDead }
    }

interface EnterNodeMarker
interface ExitNodeMarker
interface GraphEnterNodeMarker : EnterNodeMarker
interface GraphExitNodeMarker : ExitNodeMarker

// ----------------------------------- EnterNode for declaration with CFG -----------------------------------

sealed class CFGNodeWithSubgraphs<out E : FirElement>(owner: ControlFlowGraph, level: Int) : CFGNode<E>(owner, level) {
    abstract val subGraphs: List<ControlFlowGraph>
}

sealed class CFGNodeWithCfgOwner<out E : FirControlFlowGraphOwner>(owner: ControlFlowGraph, level: Int) : CFGNodeWithSubgraphs<E>(owner, level) {
    final override val subGraphs: List<ControlFlowGraph>
        get() = listOfNotNull(fir.controlFlowGraphReference?.controlFlowGraph)
}

// ----------------------------------- Named function -----------------------------------

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: FirFunction, level: Int) : CFGNode<FirFunction>(owner, level),
    GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionEnterNode(this, data)
    }
}

class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction, level: Int) : CFGNode<FirFunction>(owner, level),
    GraphExitNodeMarker, EdgeLabel {
    override val label: String
        get() = "return@${fir.symbol.callableId}"

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionExitNode(this, data)
    }
}

class LocalFunctionDeclarationNode(owner: ControlFlowGraph, override val fir: FirFunction, level: Int) : CFGNodeWithCfgOwner<FirFunction>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalFunctionDeclarationNode(this, data)
    }
}

// ----------------------------------- Default arguments -----------------------------------

class EnterValueParameterNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int) : CFGNodeWithCfgOwner<FirValueParameter>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterValueParameterNode(this, data)
    }
}

class EnterDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int) : CFGNode<FirValueParameter>(owner, level),
    GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterDefaultArgumentsNode(this, data)
    }
}

class ExitDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int) : CFGNode<FirValueParameter>(owner, level),
    GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitDefaultArgumentsNode(this, data)
    }
}

class ExitValueParameterNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int) : CFGNode<FirValueParameter>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitValueParameterNode(this, data)
    }
}

// ----------------------------------- Anonymous function -----------------------------------

class SplitPostponedLambdasNode(owner: ControlFlowGraph, override val fir: FirStatement, val lambdas: List<FirAnonymousFunction>, level: Int)
    : CFGNodeWithSubgraphs<FirStatement>(owner, level) {

    override val subGraphs: List<ControlFlowGraph>
        get() = lambdas.mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitSplitPostponedLambdasNode(this, data)
    }
}

class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunctionExpression, level: Int) : CFGNode<FirAnonymousFunctionExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPostponedLambdaExitNode(this, data)
    }
}

class MergePostponedLambdaExitsNode(owner: ControlFlowGraph, override val fir: FirElement, level: Int) : CFGNode<FirElement>(owner, level) {

    private var _flowInitialized = false
    val flowInitialized: Boolean get() = _flowInitialized
    override var flow: PersistentFlow
        get() = super.flow
        @CfgInternals
        set(value) {
            super.flow = value
            _flowInitialized = true
        }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitMergePostponedLambdaExitsNode(this, data)
    }
}

class AnonymousFunctionExpressionNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunctionExpression, level: Int) : CFGNodeWithSubgraphs<FirAnonymousFunctionExpression>(owner, level) {
    override val subGraphs: List<ControlFlowGraph>
        get() = listOfNotNull(fir.anonymousFunction.controlFlowGraphReference?.controlFlowGraph)

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousFunctionExpressionNode(this, data)
    }
}

// ----------------------------------- Files ------------------------------------------

class FileEnterNode(owner: ControlFlowGraph, override val fir: FirFile, level: Int) : CFGNodeWithSubgraphs<FirFile>(owner, level),
    GraphEnterNodeMarker {
    @set:CfgInternals
    override lateinit var subGraphs: List<ControlFlowGraph>

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFileEnterNode(this, data)
    }
}

class FileExitNode(owner: ControlFlowGraph, override val fir: FirFile, level: Int) : CFGNode<FirFile>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFileExitNode(this, data)
    }
}

// ----------------------------------- Classes -----------------------------------

class ClassEnterNode(owner: ControlFlowGraph, override val fir: FirClass, level: Int) : CFGNodeWithSubgraphs<FirClass>(owner, level),
    GraphEnterNodeMarker {
    @set:CfgInternals
    override lateinit var subGraphs: List<ControlFlowGraph>

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassEnterNode(this, data)
    }
}

class ClassExitNode(owner: ControlFlowGraph, override val fir: FirClass, level: Int) : CFGNodeWithSubgraphs<FirClass>(owner, level),
    GraphExitNodeMarker {

    override val isUnion: Boolean
        get() = fir is FirAnonymousObject && fir.classKind != ClassKind.ENUM_ENTRY

    @set:CfgInternals
    override lateinit var subGraphs: List<ControlFlowGraph>

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassExitNode(this, data)
    }
}

class LocalClassExitNode(owner: ControlFlowGraph, override val fir: FirRegularClass, level: Int) : CFGNodeWithCfgOwner<FirRegularClass>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalClassExitNode(this, data)
    }
}

class AnonymousObjectEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousObject, level: Int) : CFGNodeWithCfgOwner<FirAnonymousObject>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectEnterNode(this, data)
    }
}

class AnonymousObjectExpressionExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousObjectExpression, level: Int) : CFGNode<FirAnonymousObjectExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectExpressionExitNode(this, data)
    }
}

// ----------------------------------- Scripts ------------------------------------------

class ScriptEnterNode(owner: ControlFlowGraph, override val fir: FirScript, level: Int) : CFGNode<FirScript>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitScriptEnterNode(this, data)
    }
}

class ScriptExitNode(owner: ControlFlowGraph, override val fir: FirScript, level: Int) : CFGNode<FirScript>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitScriptExitNode(this, data)
    }
}

// ----------------------------------- Code Fragments ------------------------------------------

class CodeFragmentEnterNode(owner: ControlFlowGraph, override val fir: FirCodeFragment, level: Int) : CFGNode<FirCodeFragment>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCodeFragmentEnterNode(this, data)
    }
}

class CodeFragmentExitNode(owner: ControlFlowGraph, override val fir: FirCodeFragment, level: Int) : CFGNode<FirCodeFragment>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCodeFragmentExitNode(this, data)
    }
}
// ----------------------------------- Property -----------------------------------

class PropertyInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int) : CFGNode<FirProperty>(owner, level),
    GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerEnterNode(this, data)
    }
}

class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int) : CFGNode<FirProperty>(owner, level),
    GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerExitNode(this, data)
    }
}


class DelegateExpressionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int)
    : CFGNode<FirExpression>(owner, level) {

    override val isUnion: Boolean get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitDelegateExpressionExitNode(this, data)
    }
}

// ----------------------------------- Field -----------------------------------

class FieldInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirField, level: Int) : CFGNode<FirField>(owner, level),
    GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFieldInitializerEnterNode(this, data)
    }
}

class FieldInitializerExitNode(owner: ControlFlowGraph, override val fir: FirField, level: Int) : CFGNode<FirField>(owner, level),
    GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFieldInitializerExitNode(this, data)
    }
}

// ----------------------------------- Init -----------------------------------

class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int) : CFGNode<FirAnonymousInitializer>(owner, level),
    GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockEnterNode(this, data)
    }
}

class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int) : CFGNode<FirAnonymousInitializer>(owner, level),
    GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockExitNode(this, data)
    }
}

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int) : CFGNode<FirBlock>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockEnterNode(this, data)
    }
}
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int) : CFGNode<FirBlock>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockExitNode(this, data)
    }
}

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int) : CFGNode<FirWhenExpression>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenEnterNode(this, data)
    }
}
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int) : CFGNode<FirWhenExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenExitNode(this, data)
    }
}
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionEnterNode(this, data)
    }
}
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionExitNode(this, data)
    }
}
class WhenBranchResultEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchResultEnterNode(this, data)
    }
}
class WhenBranchResultExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchResultExitNode(this, data)
    }
}
class WhenSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int) : CFGNode<FirWhenExpression>(owner, level) {
    init {
        assert(!fir.isProperlyExhaustive)
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenSyntheticElseBranchNode(this, data)
    }
}

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopEnterNode(this, data)
    }
}
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockEnterNode(this, data)
    }
}
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockExitNode(this, data)
    }
}
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, val loop: FirLoop, level: Int) : CFGNode<FirExpression>(owner, level),
    EnterNodeMarker, EdgeLabel {
    override val label: String
        get() = loop.label?.let { "continue@${it.name}" } ?: "continue"

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionEnterNode(this, data)
    }
}
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int) : CFGNode<FirExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionExitNode(this, data)
    }
}
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level),
    ExitNodeMarker, EdgeLabel {
    override val label: String
        get() = fir.label?.let { "break@${it.name}" } ?: "break"

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopExitNode(this, data)
    }
}

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionEnterNode(this, data)
    }
}
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockEnterNode(this, data)
    }
}
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockExitNode(this, data)
    }
}
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int) : CFGNode<FirCatch>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseEnterNode(this, data)
    }
}
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int) : CFGNode<FirCatch>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseExitNode(this, data)
    }
}
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockEnterNode(this, data)
    }
}
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockExitNode(this, data)
    }
}

class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionExitNode(this, data)
    }
}

// ----------------------------------- Boolean operators -----------------------------------

abstract class AbstractBinaryExitNode<T : FirElement>(owner: ControlFlowGraph, level: Int) : CFGNode<T>(owner, level) {
    val leftOperandNode: CFGNode<*> get() = previousNodes[0]
    val rightOperandNode: CFGNode<*> get() = previousNodes[1]
}

class BinaryAndEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndEnterNode(this, data)
    }
}
class BinaryAndExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitLeftOperandNode(this, data)
    }
}
class BinaryAndEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndEnterRightOperandNode(this, data)
    }
}
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitNode(this, data)
    }
}

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level),
    EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrEnterNode(this, data)
    }
}
class BinaryOrExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrExitLeftOperandNode(this, data)
    }
}
class BinaryOrEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrEnterRightOperandNode(this, data)
    }
}
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level),
    ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrExitNode(this, data)
    }
}

// ----------------------------------- Operator call -----------------------------------

class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: FirTypeOperatorCall, level: Int) : CFGNode<FirTypeOperatorCall>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTypeOperatorCallNode(this, data)
    }
}

class ComparisonExpressionNode(owner: ControlFlowGraph, override val fir: FirComparisonExpression, level: Int) : CFGNode<FirComparisonExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitComparisonExpressionNode(this, data)
    }
}

class EqualityOperatorCallNode(owner: ControlFlowGraph, override val fir: FirEqualityOperatorCall, level: Int) : AbstractBinaryExitNode<FirEqualityOperatorCall>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEqualityOperatorCallNode(this, data)
    }
}

// ----------------------------------- Jump -----------------------------------

class JumpNode(owner: ControlFlowGraph, override val fir: FirJump<*>, level: Int) : CFGNode<FirJump<*>>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitJumpNode(this, data)
    }
}
class ConstExpressionNode(owner: ControlFlowGraph, override val fir: FirConstExpression<*>, level: Int) : CFGNode<FirConstExpression<*>>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitConstExpressionNode(this, data)
    }
}

// ----------------------------------- Check not null call -----------------------------------

class CheckNotNullCallNode(owner: ControlFlowGraph, override val fir: FirCheckNotNullCall, level: Int)
    : CFGNode<FirCheckNotNullCall>(owner, level) {
    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCheckNotNullCallNode(this, data)
    }
}

// ----------------------------------- Resolvable call -----------------------------------

class QualifiedAccessNode(
    owner: ControlFlowGraph,
    override val fir: FirQualifiedAccessExpression,
    level: Int
) : CFGNode<FirQualifiedAccessExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitQualifiedAccessNode(this, data)
    }
}

class ResolvedQualifierNode(
    owner: ControlFlowGraph,
    override val fir: FirResolvedQualifier,
    level: Int
) : CFGNode<FirResolvedQualifier>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitResolvedQualifierNode(this, data)
    }
}

class FunctionCallNode(owner: ControlFlowGraph, override val fir: FirFunctionCall, level: Int)
    : CFGNode<FirFunctionCall>(owner, level) {
    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionCallNode(this, data)
    }
}

class CallableReferenceNode(
    owner: ControlFlowGraph,
    override val fir: FirCallableReferenceAccess,
    level: Int
) : CFGNode<FirCallableReferenceAccess>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCallableReferenceNode(this, data)
    }
}

class GetClassCallNode(owner: ControlFlowGraph, override val fir: FirGetClassCall, level: Int) : CFGNode<FirGetClassCall>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitGetClassCallNode(this, data)
    }
}

class DelegatedConstructorCallNode(owner: ControlFlowGraph, override val fir: FirDelegatedConstructorCall, level: Int)
    : CFGNode<FirDelegatedConstructorCall>(owner, level) {

    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitDelegatedConstructorCallNode(this, data)
    }
}

class StringConcatenationCallNode(owner: ControlFlowGraph, override val fir: FirStringConcatenationCall, level: Int)
    : CFGNode<FirStringConcatenationCall>(owner, level) {

    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitStringConcatenationCallNode(this, data)
    }
}

class ThrowExceptionNode(
    owner: ControlFlowGraph,
    override val fir: FirThrowExpression,
    level: Int
) : CFGNode<FirThrowExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitThrowExceptionNode(this, data)
    }
}

class StubNode(owner: ControlFlowGraph, level: Int) : CFGNode<FirStub>(owner, level) {
    init {
        isDead = true
    }

    override val fir: FirStub get() = FirStub

    override var flow: PersistentFlow
        get() = firstPreviousNode.flow
        @CfgInternals
        set(_) = throw IllegalStateException("can't set flow for stub node")

    override val alternateFlowPaths: Set<FlowPath>
        get() = firstPreviousNode.alternateFlowPaths

    override fun getAlternateFlow(path: FlowPath): PersistentFlow? {
        return firstPreviousNode.getAlternateFlow(path)
    }

    @CfgInternals
    override fun addAlternateFlow(path: FlowPath, flow: PersistentFlow) {
        firstPreviousNode.addAlternateFlow(path, flow)
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitStubNode(this, data)
    }
}

class VariableDeclarationNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int) : CFGNode<FirProperty>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitVariableDeclarationNode(this, data)
    }
}
class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: FirVariableAssignment, level: Int) : CFGNode<FirVariableAssignment>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitVariableAssignmentNode(this, data)
    }
}

class EnterSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int) : CFGNode<FirSafeCallExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterSafeCallNode(this, data)
    }
}
class ExitSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int) : CFGNode<FirSafeCallExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitSafeCallNode(this, data)
    }
}

// ----------------------------------- Elvis -----------------------------------

class ElvisLhsExitNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int) : CFGNode<FirElvisExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisLhsExitNode(this, data)
    }
}

class ElvisLhsIsNotNullNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int) : CFGNode<FirElvisExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisLhsIsNotNullNode(this, data)
    }
}

class ElvisRhsEnterNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int) : CFGNode<FirElvisExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisRhsEnterNode(this, data)
    }
}

class ElvisExitNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int) : AbstractBinaryExitNode<FirElvisExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisExitNode(this, data)
    }
}

class WhenSubjectExpressionExitNode(owner: ControlFlowGraph, override val fir: FirWhenSubjectExpression, level: Int) : CFGNode<FirWhenSubjectExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenSubjectExpressionExitNode(this, data)
    }
}

// ----------------------------------- Stub -----------------------------------

object FirStub : FirExpression() {
    override val source: KtSourceElement? get() = null
    @UnresolvedExpressionTypeAccess
    override val coneTypeOrNull: ConeKotlinType = StandardClassIds.Nothing.constructClassLikeType()
    override val annotations: List<FirAnnotation> get() = listOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpression = this
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) { assert(newAnnotations.isEmpty()) }
    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) { assert(newConeTypeOrNull?.isNothing == true) }
}

class FakeExpressionEnterNode(owner: ControlFlowGraph, level: Int) : CFGNode<FirStub>(owner, level), GraphEnterNodeMarker, GraphExitNodeMarker {
    init { isDead = true }

    override val fir: FirStub = FirStub

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        throw IllegalStateException("fake expressions should not appear in graphs")
    }
}

// ----------------------------------- Smart-cast node -----------------------------------

class SmartCastExpressionExitNode(owner: ControlFlowGraph, override val fir: FirSmartCastExpression, level: Int) : CFGNode<FirSmartCastExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitSmartCastExpressionExitNode(this, data)
    }
}
