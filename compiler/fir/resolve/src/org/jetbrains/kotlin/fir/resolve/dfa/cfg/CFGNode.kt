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

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int, protected val id: Int) {
    companion object {
        fun addEdgeIfNotExist(
            from: CFGNode<*>,
            to: CFGNode<*>,
            kind: EdgeKind,
            propagateDeadness: Boolean,
            label: EdgeLabel = NormalPath
        ) {
            if (to !in from._followingNodes && from !in to.previousNodes) {
                addEdge(from, to, kind, propagateDeadness, label)
            }
        }

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

    abstract fun copy(owner: ControlFlowGraph): CFGNode<E>
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirFunction<*>> {
        return FunctionEnterNode(
            owner, fir, level, id
        )
    }
}
class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFunctionExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirFunction<*>> {
        return FunctionExitNode(
            owner, fir, level, id
        )
    }
}
class LocalFunctionDeclarationNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNodeWithCfgOwner<FirFunction<*>>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalFunctionDeclarationNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirFunction<*>> {
        return LocalFunctionDeclarationNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirValueParameter> {
        return EnterDefaultArgumentsNode(
            owner, fir, level, id
        )
    }
}

class ExitDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: FirValueParameter, level: Int, id: Int) : CFGNode<FirValueParameter>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitDefaultArgumentsNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirValueParameter> {
        return ExitDefaultArgumentsNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Anonymous function -----------------------------------

class PostponedLambdaEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNodeWithCfgOwner<FirAnonymousFunction>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPostponedLambdaEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnonymousFunction> {
        return PostponedLambdaEnterNode(
            owner, fir, level, id
        )
    }
}
class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNode<FirAnonymousFunction>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPostponedLambdaExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnonymousFunction> {
        return PostponedLambdaExitNode(
            owner, fir, level, id
        )
    }
}
class UnionFunctionCallArgumentsNode(owner: ControlFlowGraph, override val fir: FirElement, level: Int, id: Int) : CFGNode<FirElement>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitUnionFunctionCallArgumentsNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirElement> {
        return UnionFunctionCallArgumentsNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirClass<*>> {
        return ClassEnterNode(
            owner, fir, level, id
        )
    }
}

class ClassExitNode(owner: ControlFlowGraph, override val fir: FirClass<*>, level: Int, id: Int) : CFGNode<FirClass<*>>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitClassExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirClass<*>> {
        return ClassExitNode(
            owner, fir, level, id
        )
    }
}

class LocalClassExitNode(owner: ControlFlowGraph, override val fir: FirRegularClass, level: Int, id: Int) : CFGNodeWithCfgOwner<FirRegularClass>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLocalClassExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirRegularClass> {
        return LocalClassExitNode(
            owner, fir, level, id
        )
    }
}

class AnonymousObjectExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousObject, level: Int, id: Int) : CFGNodeWithCfgOwner<FirAnonymousObject>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousObjectExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnonymousObject> {
        return AnonymousObjectExitNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Initialization -----------------------------------

class PartOfClassInitializationNode(owner: ControlFlowGraph, override val fir: FirControlFlowGraphOwner, level: Int, id: Int) : CFGNodeWithCfgOwner<FirControlFlowGraphOwner>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPartOfClassInitializationNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirControlFlowGraphOwner> {
        return PartOfClassInitializationNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirProperty> {
        return PropertyInitializerEnterNode(
            owner, fir, level, id
        )
    }
}
class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitPropertyInitializerExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirProperty> {
        return PropertyInitializerExitNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnonymousInitializer> {
        return InitBlockEnterNode(
            owner, fir, level, id
        )
    }
}
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitInitBlockExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnonymousInitializer> {
        return InitBlockExitNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBlock> {
        return BlockEnterNode(
            owner, fir, level, id
        )
    }
}
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBlockExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBlock> {
        return BlockExitNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenExpression> {
        return WhenEnterNode(
            owner, fir, level, id
        )
    }
}
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenExpression> {
        return WhenExitNode(
            owner, fir, level, id
        )
    }
}
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenBranch> {
        return WhenBranchConditionEnterNode(
            owner, fir, level, id
        )
    }
}
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchConditionExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenBranch> {
        return WhenBranchConditionExitNode(
            owner, fir, level, id
        )
    }
}
class WhenBranchResultEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchResultEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenBranch> {
        return WhenBranchResultEnterNode(
            owner, fir, level, id
        )
    }
}
class WhenBranchResultExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenBranchResultExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenBranch> {
        return WhenBranchResultEnterNode(
            owner, fir, level, id
        )
    }
}
class WhenSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id) {
    init {
        assert(!fir.isExhaustive)
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitWhenSyntheticElseBranchNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirWhenExpression> {
        return WhenSyntheticElseBranchNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirLoop> {
        return LoopEnterNode(
            owner, fir, level, id
        )
    }
}
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirLoop> {
        return LoopBlockEnterNode(
            owner, fir, level, id
        )
    }
}
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopBlockExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirLoop> {
        return LoopBlockExitNode(
            owner, fir, level, id
        )
    }
}
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirExpression> {
        return LoopConditionEnterNode(
            owner, fir, level, id
        )
    }
}
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopConditionExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirExpression> {
        return LoopConditionExitNode(
            owner, fir, level, id
        )
    }
}
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitLoopExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirLoop> {
        return LoopExitNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return TryExpressionEnterNode(
            owner, fir, level, id
        )
    }
}
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return TryMainBlockEnterNode(
            owner, fir, level, id
        )
    }
}
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryMainBlockExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return TryMainBlockExitNode(
            owner, fir, level, id
        )
    }
}
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirCatch> {
        return CatchClauseEnterNode(
            owner, fir, level, id
        )
    }
}
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCatchClauseExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirCatch> {
        return CatchClauseExitNode(
            owner, fir, level, id
        )
    }
}
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return FinallyBlockEnterNode(
            owner, fir, level, id
        )
    }
}
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyBlockExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return FinallyBlockExitNode(
            owner, fir, level, id
        )
    }
}
class FinallyProxyEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyProxyEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return FinallyBlockEnterNode(
            owner, fir, level, id
        )
    }
}
class FinallyProxyExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitFinallyProxyExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return FinallyProxyExitNode(
            owner, fir, level, id
        )
    }
}
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTryExpressionExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTryExpression> {
        return TryExpressionExitNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryAndEnterNode(
            owner, fir, level, id
        )
    }
}
class BinaryAndExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitLeftOperandNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryAndExitLeftOperandNode(
            owner, fir, level, id
        )
    }
}
class BinaryAndEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndEnterRightOperandNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryAndEnterRightOperandNode(
            owner, fir, level, id
        )
    }
}
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryAndExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryAndEnterNode(
            owner, fir, level, id
        )
    }
}

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryOrEnterNode(
            owner, fir, level, id
        )
    }
}
class BinaryOrExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrExitLeftOperandNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryOrExitLeftOperandNode(
            owner, fir, level, id
        )
    }
}
class BinaryOrEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrEnterRightOperandNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryAndEnterRightOperandNode(
            owner, fir, level, id
        )
    }
}
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitBinaryOrExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirBinaryLogicExpression> {
        return BinaryOrExitNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Operator call -----------------------------------

class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: FirTypeOperatorCall, level: Int, id: Int) : CFGNode<FirTypeOperatorCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitTypeOperatorCallNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirTypeOperatorCall> {
        return TypeOperatorCallNode(
            owner, fir, level, id
        )
    }
}

class ComparisonExpressionNode(owner: ControlFlowGraph, override val fir: FirComparisonExpression, level: Int, id: Int) : CFGNode<FirComparisonExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitComparisonExpressionNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirComparisonExpression> {
        return ComparisonExpressionNode(
            owner, fir, level, id
        )
    }
}

class EqualityOperatorCallNode(owner: ControlFlowGraph, override val fir: FirEqualityOperatorCall, level: Int, id: Int) : AbstractBinaryExitNode<FirEqualityOperatorCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEqualityOperatorCallNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirEqualityOperatorCall> {
        return EqualityOperatorCallNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Jump -----------------------------------

class JumpNode(owner: ControlFlowGraph, override val fir: FirJump<*>, level: Int, id: Int) : CFGNode<FirJump<*>>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitJumpNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirJump<*>> {
        return JumpNode(
            owner, fir, level, id
        )
    }
}
class ConstExpressionNode(owner: ControlFlowGraph, override val fir: FirConstExpression<*>, level: Int, id: Int) : CFGNode<FirConstExpression<*>>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitConstExpressionNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirConstExpression<*>> {
        return ConstExpressionNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Check not null call -----------------------------------

class CheckNotNullCallNode(owner: ControlFlowGraph, override val fir: FirCheckNotNullCall, level: Int, id: Int) : CFGNode<FirCheckNotNullCall>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitCheckNotNullCallNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirCheckNotNullCall> {
        return CheckNotNullCallNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirQualifiedAccessExpression> {
        return QualifiedAccessNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirResolvedQualifier> {
        return ResolvedQualifierNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirFunctionCall> {
        return FunctionCallNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirCallableReferenceAccess> {
        return CallableReferenceNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirDelegatedConstructorCall> {
        return DelegatedConstructorCallNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirThrowExpression> {
        return ThrowExceptionNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirStub> {
        return StubNode(
            owner, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirStub> {
        return ContractDescriptionEnterNode(
            owner, level, id
        )
    }
}

class VariableDeclarationNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitVariableDeclarationNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirProperty> {
        return VariableDeclarationNode(
            owner, fir, level, id
        )
    }
}
class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: FirVariableAssignment, level: Int, id: Int) : CFGNode<FirVariableAssignment>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitVariableAssignmentNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirVariableAssignment> {
        return VariableAssignmentNode(
            owner, fir, level, id
        )
    }
}

class EnterContractNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterContractNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirQualifiedAccess> {
        return EnterContractNode(
            owner, fir, level, id
        )
    }
}
class ExitContractNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitContractNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirQualifiedAccess> {
        return ExitContractNode(
            owner, fir, level, id
        )
    }
}

class EnterSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int, id: Int) : CFGNode<FirSafeCallExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitEnterSafeCallNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirSafeCallExpression> {
        return EnterSafeCallNode(
            owner, fir, level, id
        )
    }
}
class ExitSafeCallNode(owner: ControlFlowGraph, override val fir: FirSafeCallExpression, level: Int, id: Int) : CFGNode<FirSafeCallExpression>(owner, level, id) {
    val lastPreviousNode: CFGNode<*> get() = previousNodes.last()
    val secondPreviousNode: CFGNode<*>? get() = previousNodes.getOrNull(1)

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitExitSafeCallNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirSafeCallExpression> {
        return ExitSafeCallNode(
            owner, fir, level, id
        )
    }
}

// ----------------------------------- Elvis -----------------------------------

class ElvisLhsExitNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : CFGNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisLhsExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirElvisExpression> {
        return ElvisLhsExitNode(
            owner, fir, level, id
        )
    }
}

class ElvisLhsIsNotNullNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : CFGNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisLhsIsNotNullNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirElvisExpression> {
        return ElvisLhsIsNotNullNode(
            owner, fir, level, id
        )
    }
}

class ElvisRhsEnterNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : CFGNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisRhsEnterNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirElvisExpression> {
        return ElvisRhsEnterNode(
            owner, fir, level, id
        )
    }
}

class ElvisExitNode(owner: ControlFlowGraph, override val fir: FirElvisExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirElvisExpression>(owner, level, id) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitElvisExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirElvisExpression> {
        return ElvisExitNode(
            owner, fir, level, id
        )
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

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnnotationCall> {
        return AnnotationEnterNode(
            owner, fir, level, id
        )
    }
}
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), ExitNodeMarker {
    init {
        owner.exitNode = this
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R {
        return visitor.visitAnnotationExitNode(this, data)
    }

    override fun copy(owner: ControlFlowGraph): CFGNode<FirAnnotationCall> {
        return AnnotationExitNode(
            owner, fir, level, id
        )
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
