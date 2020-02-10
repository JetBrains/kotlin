/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Reformat")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class ControlFlowGraph(val name: String, val kind: Kind) {
    private val _nodes: MutableList<CFGNode<*>> = mutableListOf()

    val nodes: List<CFGNode<*>> get() = _nodes

    internal fun addNode(node: CFGNode<*>) {
        _nodes += node
    }

    lateinit var enterNode: CFGNode<*>
        internal set
    lateinit var exitNode: CFGNode<*>
        internal set

    enum class Kind {
        Function, ClassInitializer, PropertyInitializer, TopLevel
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
            if (kind != EdgeKind.Simple) {
                from._outgoingEdges[to] = kind
                to._incomingEdges[from] = kind
            }
            if (propagateDeadness && kind == EdgeKind.Dead) {
                to.isDead = true
            }
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

    final override fun equals(other: Any?): Boolean {
        if (other !is CFGNode<*>) return false
        return this.id == other.id
    }

    final override fun hashCode(): Int {
        return id
    }
}

val CFGNode<*>.firstPreviousNode: CFGNode<*> get() = previousNodes[0]

interface EnterNodeMarker
interface ExitNodeMarker

// ----------------------------------- Named function -----------------------------------

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), EnterNodeMarker
class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), ExitNodeMarker

// ----------------------------------- Anonymous function -----------------------------------

class PostponedLambdaEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNode<FirAnonymousFunction>(owner, level, id)
class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousFunction, level: Int, id: Int) : CFGNode<FirAnonymousFunction>(owner, level, id)

// ----------------------------------- Property -----------------------------------

class PropertyInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), EnterNodeMarker
class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), ExitNodeMarker

// ----------------------------------- Init -----------------------------------

class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), EnterNodeMarker
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), ExitNodeMarker

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), EnterNodeMarker
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), ExitNodeMarker

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), EnterNodeMarker
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), ExitNodeMarker
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), EnterNodeMarker
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), ExitNodeMarker
class WhenBranchResultEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id)
class WhenBranchResultExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id)
class WhenSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id) {
    init {
        assert(!fir.isExhaustive)
    }
}

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNodeMarker
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNodeMarker
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNodeMarker
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), EnterNodeMarker
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), ExitNodeMarker
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNodeMarker

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), EnterNodeMarker
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), ExitNodeMarker
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker
class FinallyProxyEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNodeMarker
class FinallyProxyExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNodeMarker

// ----------------------------------- Boolean operators -----------------------------------

abstract class AbstractBinaryExitNode<T : FirElement>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<T>(owner, level, id) {
    val leftOperandNode: CFGNode<*> get() = previousNodes[0]
    val rightOperandNode: CFGNode<*> get() = previousNodes[1]
}

class BinaryAndEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNodeMarker
class BinaryAndExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryAndEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNodeMarker

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNodeMarker
class BinaryOrExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryOrEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNodeMarker

// ----------------------------------- Operator call -----------------------------------

class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: FirTypeOperatorCall, level: Int, id: Int) : CFGNode<FirTypeOperatorCall>(owner, level, id)
class OperatorCallNode(owner: ControlFlowGraph, override val fir: FirOperatorCall, level: Int, id: Int) : AbstractBinaryExitNode<FirOperatorCall>(owner, level, id)

// ----------------------------------- Jump -----------------------------------

class JumpNode(owner: ControlFlowGraph, override val fir: FirJump<*>, level: Int, id: Int) : CFGNode<FirJump<*>>(owner, level, id)
class ConstExpressionNode(owner: ControlFlowGraph, override val fir: FirConstExpression<*>, level: Int, id: Int) : CFGNode<FirConstExpression<*>>(owner, level, id)

// ----------------------------------- Check not null call -----------------------------------

class CheckNotNullCallNode(owner: ControlFlowGraph, override val fir: FirCheckNotNullCall, level: Int, id: Int) : CFGNode<FirCheckNotNullCall>(owner, level, id)

// ----------------------------------- Resolvable call -----------------------------------

class QualifiedAccessNode(
    owner: ControlFlowGraph,
    override val fir: FirQualifiedAccessExpression,
    level: Int, id: Int
) : CFGNode<FirQualifiedAccessExpression>(owner, level, id)

class FunctionCallNode(
    owner: ControlFlowGraph,
    override val fir: FirFunctionCall,
    level: Int, id: Int
) : CFGNode<FirFunctionCall>(owner, level, id)

class ThrowExceptionNode(
    owner: ControlFlowGraph,
    override val fir: FirThrowExpression,
    level: Int, id: Int
) : CFGNode<FirThrowExpression>(owner, level, id)

class StubNode(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<FirStub>(owner, level, id) {
    init {
        isDead = true
    }

    override val fir: FirStub get() = FirStub
}

class VariableDeclarationNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id)
class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: FirVariableAssignment, level: Int, id: Int) : CFGNode<FirVariableAssignment>(owner, level, id)

class EnterContractNode(owner: ControlFlowGraph, override val fir: FirFunctionCall, level: Int, id: Int) : CFGNode<FirFunctionCall>(owner, level, id), EnterNodeMarker
class ExitContractNode(owner: ControlFlowGraph, override val fir: FirFunctionCall, level: Int, id: Int) : CFGNode<FirFunctionCall>(owner, level, id), ExitNodeMarker

class EnterSafeCallNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id)
class ExitSafeCallNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id) {
    val lastPreviousNode: CFGNode<*> get() = previousNodes.last()
    val secondPreviousNode: CFGNode<*>? get() = previousNodes.getOrNull(1)
}

// ----------------------------------- Other -----------------------------------

class AnnotationEnterNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), EnterNodeMarker
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), ExitNodeMarker

// ----------------------------------- Stub -----------------------------------

object FirStub : FirElement {
    override val source: FirSourceElement? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}
