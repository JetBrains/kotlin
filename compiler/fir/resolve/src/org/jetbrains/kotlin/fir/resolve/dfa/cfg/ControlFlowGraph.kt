/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class ControlFlowGraph(val name: String, val kind: Kind) {
    val nodes = mutableListOf<CFGNode<*>>()
    lateinit var enterNode: CFGNode<*>
    lateinit var exitNode: CFGNode<*>

    enum class Kind {
        Function, ClassInitializer, PropertyInitializer, TopLevel
    }
}

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int, private val id: Int) {
    init {
        owner.nodes += this
    }

    val previousNodes = mutableListOf<CFGNode<*>>()
    val followingNodes = mutableListOf<CFGNode<*>>()

    val firstPreviousNode: CFGNode<*> get() = previousNodes.first()

    abstract val fir: E
    var isDead: Boolean = false

    final override fun equals(other: Any?): Boolean {
        if (other !is CFGNode<*>) return false
        return this.id == other.id
    }

    final override fun hashCode(): Int {
        return id
    }
}


interface EnterNode
interface ExitNode

// ----------------------------------- Named function -----------------------------------

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), EnterNode
class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int, id: Int) : CFGNode<FirFunction<*>>(owner, level, id), ExitNode

// ----------------------------------- Property -----------------------------------

class PropertyInitializerEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), EnterNode
class PropertyInitializerExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int, id: Int) : CFGNode<FirProperty>(owner, level, id), ExitNode

// ----------------------------------- Init -----------------------------------

class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), EnterNode
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int, id: Int) : CFGNode<FirAnonymousInitializer>(owner, level, id), ExitNode

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), EnterNode
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int, id: Int) : CFGNode<FirBlock>(owner, level, id), ExitNode

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), EnterNode
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id), ExitNode
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), EnterNode
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id), ExitNode
class WhenBranchResultEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id)
class WhenBranchResultExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int, id: Int) : CFGNode<FirWhenBranch>(owner, level, id)
class WhenSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int, id: Int) : CFGNode<FirWhenExpression>(owner, level, id) {
    init {
        assert(!fir.isExhaustive)
    }
}

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNode
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), EnterNode
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNode
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), EnterNode
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int, id: Int) : CFGNode<FirExpression>(owner, level, id), ExitNode
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int, id: Int) : CFGNode<FirLoop>(owner, level, id), ExitNode

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNode
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNode
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNode
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), EnterNode
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int, id: Int) : CFGNode<FirCatch>(owner, level, id), ExitNode
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNode
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNode
class FinallyProxyEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), EnterNode
class FinallyProxyExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNode
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int, id: Int) : CFGNode<FirTryExpression>(owner, level, id), ExitNode

// ----------------------------------- Boolean operators -----------------------------------

abstract class AbstractBinaryExitNode<T : FirElement>(owner: ControlFlowGraph, level: Int, id: Int) : CFGNode<T>(owner, level, id) {
    val leftOperandNode: CFGNode<*> get() = previousNodes[0]
    val rightOperandNode: CFGNode<*> get() = previousNodes[1]
}

class BinaryAndEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNode
class BinaryAndExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryAndEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNode

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id), EnterNode
class BinaryOrExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryOrEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : CFGNode<FirBinaryLogicExpression>(owner, level, id)
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int, id: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level, id), ExitNode

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

class EnterContractNode(owner: ControlFlowGraph, override val fir: FirFunctionCall, level: Int, id: Int) : CFGNode<FirFunctionCall>(owner, level, id), EnterNode
class ExitContractNode(owner: ControlFlowGraph, override val fir: FirFunctionCall, level: Int, id: Int) : CFGNode<FirFunctionCall>(owner, level, id), ExitNode

class EnterSafeCallNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id)
class ExitSafeCallNode(owner: ControlFlowGraph, override val fir: FirQualifiedAccess, level: Int, id: Int) : CFGNode<FirQualifiedAccess>(owner, level, id) {
    val lastPreviousNode: CFGNode<*> get() = previousNodes.last()
    val secondPreviousNode: CFGNode<*>? get() = previousNodes.getOrNull(1)
}

// ----------------------------------- Other -----------------------------------

class AnnotationEnterNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), EnterNode
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int, id: Int) : CFGNode<FirAnnotationCall>(owner, level, id), ExitNode

// ----------------------------------- Stub -----------------------------------

object FirStub : FirElement {
    override val source: FirSourceElement? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}
