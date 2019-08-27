/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*

abstract class ControlFlowGraphNodeBuilder {
    protected abstract val graph: ControlFlowGraph
    protected abstract var levelCounter: Int

    protected fun createStubNode(): StubNode = StubNode(
        graph,
        levelCounter
    )

    protected fun createLoopExitNode(fir: FirLoop): LoopExitNode = LoopExitNode(graph, fir, levelCounter)

    protected fun createLoopEnterNode(fir: FirLoop): LoopEnterNode = LoopEnterNode(graph, fir, levelCounter)

    protected fun createInitBlockExitNode(fir: FirAnonymousInitializer): InitBlockExitNode =
        InitBlockExitNode(graph, fir, levelCounter)

    protected fun createInitBlockEnterNode(fir: FirAnonymousInitializer): InitBlockEnterNode =
        InitBlockEnterNode(graph, fir, levelCounter)

    protected fun createTypeOperatorCallNode(fir: FirTypeOperatorCall): TypeOperatorCallNode =
        TypeOperatorCallNode(graph, fir, levelCounter)

    protected fun createOperatorCallNode(fir: FirOperatorCall): OperatorCallNode =
        OperatorCallNode(graph, fir, levelCounter)

    protected fun createWhenBranchConditionExitNode(fir: FirWhenBranch): WhenBranchConditionExitNode =
        WhenBranchConditionExitNode(graph, fir, levelCounter)

    protected fun createJumpNode(fir: FirJump<*>): JumpNode =
        JumpNode(graph, fir, levelCounter)

    protected fun createQualifiedAccessNode(
        fir: FirQualifiedAccessExpression,
        returnsNothing: Boolean
    ): QualifiedAccessNode = QualifiedAccessNode(graph, fir, returnsNothing, levelCounter)

    protected fun createBlockEnterNode(fir: FirBlock): BlockEnterNode = BlockEnterNode(graph, fir, levelCounter)

    protected fun createBlockExitNode(fir: FirBlock): BlockExitNode = BlockExitNode(graph, fir, levelCounter)

    protected fun createPropertyExitNode(fir: FirProperty): PropertyExitNode = PropertyExitNode(graph, fir, levelCounter)

    protected fun createPropertyEnterNode(fir: FirProperty): PropertyEnterNode = PropertyEnterNode(graph, fir, levelCounter)

    protected fun createFunctionEnterNode(fir: FirFunction<*>, isInPlace: Boolean): FunctionEnterNode =
        FunctionEnterNode(graph, fir, levelCounter).also {
            if (!isInPlace) {
                graph.enterNode = it
            }
        }

    protected fun createFunctionExitNode(fir: FirFunction<*>, isInPlace: Boolean): FunctionExitNode =
        FunctionExitNode(graph, fir, levelCounter).also {
            if (!isInPlace) {
                graph.exitNode = it
            }
    }

    protected fun createBinaryOrEnterNode(fir: FirBinaryLogicExpression): BinaryOrEnterNode =
        BinaryOrEnterNode(graph, fir, levelCounter)

    protected fun createBinaryOrExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryOrExitLeftOperandNode =
        BinaryOrExitLeftOperandNode(graph, fir, levelCounter)

    protected fun createBinaryOrExitNode(fir: FirBinaryLogicExpression): BinaryOrExitNode =
        BinaryOrExitNode(graph, fir, levelCounter)

    protected fun createBinaryAndExitNode(fir: FirBinaryLogicExpression): BinaryAndExitNode =
        BinaryAndExitNode(graph, fir, levelCounter)

    protected fun createBinaryAndEnterNode(fir: FirBinaryLogicExpression): BinaryAndEnterNode =
        BinaryAndEnterNode(graph, fir, levelCounter)

    protected fun createWhenBranchConditionEnterNode(fir: FirWhenBranch): WhenBranchConditionEnterNode =
        WhenBranchConditionEnterNode(graph, fir, levelCounter)

    protected fun createWhenEnterNode(fir: FirWhenExpression): WhenEnterNode =
        WhenEnterNode(graph, fir, levelCounter)

    protected fun createWhenExitNode(fir: FirWhenExpression): WhenExitNode =
        WhenExitNode(graph, fir, levelCounter)

    protected fun createWhenBranchResultExitNode(fir: FirWhenBranch): WhenBranchResultExitNode =
        WhenBranchResultExitNode(graph, fir, levelCounter)

    protected fun createLoopConditionExitNode(fir: FirExpression): LoopConditionExitNode =
        LoopConditionExitNode(graph, fir, levelCounter)

    protected fun createLoopConditionEnterNode(fir: FirExpression): LoopConditionEnterNode =
        LoopConditionEnterNode(graph, fir, levelCounter)

    protected fun createLoopBlockEnterNode(fir: FirLoop): LoopBlockEnterNode =
        LoopBlockEnterNode(graph, fir, levelCounter)

    protected fun createLoopBlockExitNode(fir: FirLoop): LoopBlockExitNode =
        LoopBlockExitNode(graph, fir, levelCounter)

    protected fun createFunctionCallNode(fir: FirFunctionCall, returnsNothing: Boolean): FunctionCallNode =
        FunctionCallNode(graph, fir, returnsNothing, levelCounter)

    protected fun createVariableAssignmentNode(fir: FirVariableAssignment): VariableAssignmentNode =
        VariableAssignmentNode(graph, fir, levelCounter)

    protected fun createAnnotationExitNode(fir: FirAnnotationCall): AnnotationExitNode =
        AnnotationExitNode(graph, fir, levelCounter)

    protected fun createAnnotationEnterNode(fir: FirAnnotationCall): AnnotationEnterNode =
        AnnotationEnterNode(graph, fir, levelCounter)

    protected fun createVariableDeclarationNode(fir: FirVariable<*>): VariableDeclarationNode =
        VariableDeclarationNode(graph, fir, levelCounter)

    protected fun createConstExpressionNode(fir: FirConstExpression<*>): ConstExpressionNode =
        ConstExpressionNode(graph, fir, levelCounter)

    protected fun createThrowExceptionNode(fir: FirThrowExpression): ThrowExceptionNode =
        ThrowExceptionNode(graph, fir, levelCounter)

    protected fun createFinallyProxyExitNode(fir: FirTryExpression): FinallyProxyExitNode =
        FinallyProxyExitNode(graph, fir, levelCounter)

    protected fun createFinallyProxyEnterNode(fir: FirTryExpression): FinallyProxyEnterNode =
        FinallyProxyEnterNode(graph, fir, levelCounter)

    protected fun createFinallyBlockExitNode(fir: FirTryExpression): FinallyBlockExitNode =
        FinallyBlockExitNode(graph, fir, levelCounter)

    protected fun createFinallyBlockEnterNode(fir: FirTryExpression): FinallyBlockEnterNode =
        FinallyBlockEnterNode(graph, fir, levelCounter)

    protected fun createCatchClauseExitNode(fir: FirCatch): CatchClauseExitNode =
        CatchClauseExitNode(graph, fir, levelCounter)

    protected fun createTryMainBlockExitNode(fir: FirTryExpression): TryMainBlockExitNode =
        TryMainBlockExitNode(graph, fir, levelCounter)

    protected fun createTryMainBlockEnterNode(fir: FirTryExpression): TryMainBlockEnterNode =
        TryMainBlockEnterNode(graph, fir, levelCounter)

    protected fun createCatchClauseEnterNode(fir: FirCatch): CatchClauseEnterNode =
        CatchClauseEnterNode(graph, fir, levelCounter)

    protected fun createTryExpressionEnterNode(fir: FirTryExpression): TryExpressionEnterNode =
        TryExpressionEnterNode(graph, fir, levelCounter)

    protected fun createTryExpressionExitNode(fir: FirTryExpression): TryExpressionExitNode =
        TryExpressionExitNode(graph, fir, levelCounter)

}