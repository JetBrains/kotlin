/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowVariable

class ControlFlowGraph(val name: String) {
    val nodes = mutableListOf<CFGNode<*>>()
    lateinit var enterNode: CFGNode<*>
    lateinit var exitNode: CFGNode<*>
}

sealed class CFGNode<out E : FirElement>(val owner: ControlFlowGraph, val level: Int) {
    init {
        owner.nodes += this
    }

    val previousNodes = mutableListOf<CFGNode<*>>()
    val followingNodes = mutableListOf<CFGNode<*>>()

    abstract val fir: E
    var isDead: Boolean = false
}

val CFGNode<*>.usefulFollowingNodes: List<CFGNode<*>> get() = if (isDead) followingNodes else followingNodes.filterNot { it.isDead }
val CFGNode<*>.usefulPreviousNodes: List<CFGNode<*>> get() = if (isDead) previousNodes else previousNodes.filterNot { it.isDead }

interface ReturnableNothingNode {
    val returnsNothing: Boolean
}

interface EnterNode
interface ExitNode

// ----------------------------------- Named function -----------------------------------

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int) : CFGNode<FirFunction<*>>(owner, level), EnterNode
class FunctionExitNode(owner: ControlFlowGraph, override val fir: FirFunction<*>, level: Int) : CFGNode<FirFunction<*>>(owner, level), ExitNode

// ----------------------------------- Property -----------------------------------

class PropertyEnterNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int) : CFGNode<FirProperty>(owner, level), EnterNode
class PropertyExitNode(owner: ControlFlowGraph, override val fir: FirProperty, level: Int) : CFGNode<FirProperty>(owner, level), ExitNode

// ----------------------------------- Init -----------------------------------

class InitBlockEnterNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int) : CFGNode<FirAnonymousInitializer>(owner, level), EnterNode
class InitBlockExitNode(owner: ControlFlowGraph, override val fir: FirAnonymousInitializer, level: Int) : CFGNode<FirAnonymousInitializer>(owner, level), ExitNode

// ----------------------------------- Block -----------------------------------

class BlockEnterNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int) : CFGNode<FirBlock>(owner, level), EnterNode
class BlockExitNode(owner: ControlFlowGraph, override val fir: FirBlock, level: Int) : CFGNode<FirBlock>(owner, level), ExitNode

// ----------------------------------- When -----------------------------------

class WhenEnterNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int) : CFGNode<FirWhenExpression>(owner, level), EnterNode
class WhenExitNode(owner: ControlFlowGraph, override val fir: FirWhenExpression, level: Int) : CFGNode<FirWhenExpression>(owner, level), ExitNode
class WhenBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level), EnterNode
class WhenBranchConditionExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level), ExitNode {
    lateinit var variable: DataFlowVariable
}
class WhenBranchResultExitNode(owner: ControlFlowGraph, override val fir: FirWhenBranch, level: Int) : CFGNode<FirWhenBranch>(owner, level)

// ----------------------------------- Loop -----------------------------------

class LoopEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level), EnterNode
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level), EnterNode
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level), ExitNode
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int) : CFGNode<FirExpression>(owner, level), EnterNode
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: FirExpression, level: Int) : CFGNode<FirExpression>(owner, level), ExitNode
class LoopExitNode(owner: ControlFlowGraph, override val fir: FirLoop, level: Int) : CFGNode<FirLoop>(owner, level), ExitNode

// ----------------------------------- Try-catch-finally -----------------------------------

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), EnterNode
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), EnterNode
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), ExitNode
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int) : CFGNode<FirCatch>(owner, level), EnterNode
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: FirCatch, level: Int) : CFGNode<FirCatch>(owner, level), ExitNode
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), EnterNode
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), ExitNode
class FinallyProxyEnterNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), EnterNode
class FinallyProxyExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), ExitNode
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: FirTryExpression, level: Int) : CFGNode<FirTryExpression>(owner, level), ExitNode

// ----------------------------------- Boolean operators -----------------------------------

abstract class AbstractBinaryExitNode<T : FirElement>(owner: ControlFlowGraph, level: Int) : CFGNode<T>(owner, level) {
    val leftOperandNode: CFGNode<*> get() = previousNodes[0]
    val rightOperandNode: CFGNode<*> get() = previousNodes[1]
}

class BinaryAndEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level), EnterNode
class BinaryAndExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level)
class BinaryAndEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level)
class BinaryAndExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level), ExitNode

class BinaryOrEnterNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level), EnterNode
class BinaryOrExitLeftOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level)
class BinaryOrEnterRightOperandNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : CFGNode<FirBinaryLogicExpression>(owner, level)
class BinaryOrExitNode(owner: ControlFlowGraph, override val fir: FirBinaryLogicExpression, level: Int) : AbstractBinaryExitNode<FirBinaryLogicExpression>(owner, level), ExitNode

// ----------------------------------- Operator call -----------------------------------

class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: FirTypeOperatorCall, level: Int) : CFGNode<FirTypeOperatorCall>(owner, level)
class OperatorCallNode(owner: ControlFlowGraph, override val fir: FirOperatorCall, level: Int) : AbstractBinaryExitNode<FirOperatorCall>(owner, level)

// ----------------------------------- Jump -----------------------------------

class JumpNode(owner: ControlFlowGraph, override val fir: FirJump<*>, level: Int) : CFGNode<FirJump<*>>(owner, level)
class ConstExpressionNode(owner: ControlFlowGraph, override val fir: FirConstExpression<*>, level: Int) : CFGNode<FirConstExpression<*>>(owner, level)

// ----------------------------------- Resolvable call -----------------------------------

class QualifiedAccessNode(
    owner: ControlFlowGraph,
    override val fir: FirQualifiedAccessExpression,
    override val returnsNothing: Boolean,
    level: Int
) : CFGNode<FirQualifiedAccessExpression>(owner, level), ReturnableNothingNode

class FunctionCallNode(
    owner: ControlFlowGraph,
    override val fir: FirFunctionCall,
    override val returnsNothing: Boolean,
    level: Int
) : CFGNode<FirFunctionCall>(owner, level), ReturnableNothingNode

class ThrowExceptionNode(
    owner: ControlFlowGraph,
    override val fir: FirThrowExpression,
    level: Int
) : CFGNode<FirThrowExpression>(owner, level), ReturnableNothingNode {
    override val returnsNothing: Boolean get() = true
}

class StubNode(owner: ControlFlowGraph, level: Int) : CFGNode<FirStub>(owner, level) {
    init {
        isDead = true
    }

    override val fir: FirStub get() = FirStub
}

class VariableDeclarationNode(owner: ControlFlowGraph, override val fir: FirVariable<*>, level: Int) : CFGNode<FirVariable<*>>(owner, level)
class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: FirVariableAssignment, level: Int) : CFGNode<FirVariableAssignment>(owner, level)

// ----------------------------------- Other -----------------------------------

class AnnotationEnterNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int) : CFGNode<FirAnnotationCall>(owner, level), EnterNode
class AnnotationExitNode(owner: ControlFlowGraph, override val fir: FirAnnotationCall, level: Int) : CFGNode<FirAnnotationCall>(owner, level), ExitNode

// ----------------------------------- Stub -----------------------------------

object FirStub : FirElement {
    override val psi: PsiElement? get() = null
}
