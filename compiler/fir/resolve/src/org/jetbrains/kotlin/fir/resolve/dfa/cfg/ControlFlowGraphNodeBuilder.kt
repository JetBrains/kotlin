/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*

fun ControlFlowGraphBuilder.createStubNode(): StubNode = StubNode(graph, levelCounter)

fun ControlFlowGraphBuilder.createLoopExitNode(fir: FirLoop): LoopExitNode = LoopExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopEnterNode(fir: FirLoop): LoopEnterNode = LoopEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createInitBlockExitNode(fir: FirAnonymousInitializer): InitBlockExitNode =
    InitBlockExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createInitBlockEnterNode(fir: FirAnonymousInitializer): InitBlockEnterNode =
    InitBlockEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTypeOperatorCallNode(fir: FirTypeOperatorCall): TypeOperatorCallNode =
    TypeOperatorCallNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createOperatorCallNode(fir: FirOperatorCall): OperatorCallNode =
    OperatorCallNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchConditionExitNode(fir: FirWhenBranch): WhenBranchConditionExitNode =
    WhenBranchConditionExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createJumpNode(fir: FirJump<*>): JumpNode =
    JumpNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createQualifiedAccessNode(fir: FirQualifiedAccessExpression): QualifiedAccessNode =
    QualifiedAccessNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBlockEnterNode(fir: FirBlock): BlockEnterNode = BlockEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBlockExitNode(fir: FirBlock): BlockExitNode = BlockExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createPropertyInitializerExitNode(fir: FirProperty): PropertyInitializerExitNode =
    PropertyInitializerExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createPropertyInitializerEnterNode(fir: FirProperty): PropertyInitializerEnterNode =
    PropertyInitializerEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionEnterNode(fir: FirFunction<*>, isInPlace: Boolean): FunctionEnterNode =
    FunctionEnterNode(graph, fir, levelCounter).also {
        if (!isInPlace) {
            graph.enterNode = it
        }
    }

fun ControlFlowGraphBuilder.createFunctionExitNode(fir: FirFunction<*>, isInPlace: Boolean): FunctionExitNode =
    FunctionExitNode(graph, fir, levelCounter).also {
        if (!isInPlace) {
            graph.exitNode = it
        }
    }

fun ControlFlowGraphBuilder.createBinaryOrEnterNode(fir: FirBinaryLogicExpression): BinaryOrEnterNode =
    BinaryOrEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryOrExitLeftOperandNode =
    BinaryOrExitLeftOperandNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrExitNode(fir: FirBinaryLogicExpression): BinaryOrExitNode =
    BinaryOrExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndExitNode(fir: FirBinaryLogicExpression): BinaryAndExitNode =
    BinaryAndExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndEnterNode(fir: FirBinaryLogicExpression): BinaryAndEnterNode =
    BinaryAndEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchConditionEnterNode(fir: FirWhenBranch): WhenBranchConditionEnterNode =
    WhenBranchConditionEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenEnterNode(fir: FirWhenExpression): WhenEnterNode =
    WhenEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenExitNode(fir: FirWhenExpression): WhenExitNode =
    WhenExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchResultExitNode(fir: FirWhenBranch): WhenBranchResultExitNode =
    WhenBranchResultExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenSyntheticElseBranchNode(fir: FirWhenExpression): WhenSyntheticElseBranchNode =
    WhenSyntheticElseBranchNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchResultEnterNode(fir: FirWhenBranch): WhenBranchResultEnterNode =
    WhenBranchResultEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopConditionExitNode(fir: FirExpression): LoopConditionExitNode =
    LoopConditionExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopConditionEnterNode(fir: FirExpression): LoopConditionEnterNode =
    LoopConditionEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopBlockEnterNode(fir: FirLoop): LoopBlockEnterNode =
    LoopBlockEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopBlockExitNode(fir: FirLoop): LoopBlockExitNode =
    LoopBlockExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionCallNode(fir: FirFunctionCall): FunctionCallNode =
    FunctionCallNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createVariableAssignmentNode(fir: FirVariableAssignment): VariableAssignmentNode =
    VariableAssignmentNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createAnnotationExitNode(fir: FirAnnotationCall): AnnotationExitNode =
    AnnotationExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createAnnotationEnterNode(fir: FirAnnotationCall): AnnotationEnterNode =
    AnnotationEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createVariableDeclarationNode(fir: FirProperty): VariableDeclarationNode =
    VariableDeclarationNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitContractNode(fir: FirFunctionCall): ExitContractNode =
    ExitContractNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterContractNode(fir: FirFunctionCall): EnterContractNode =
    EnterContractNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createConstExpressionNode(fir: FirConstExpression<*>): ConstExpressionNode =
    ConstExpressionNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createThrowExceptionNode(fir: FirThrowExpression): ThrowExceptionNode =
    ThrowExceptionNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyProxyExitNode(fir: FirTryExpression): FinallyProxyExitNode =
    FinallyProxyExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyProxyEnterNode(fir: FirTryExpression): FinallyProxyEnterNode =
    FinallyProxyEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyBlockExitNode(fir: FirTryExpression): FinallyBlockExitNode =
    FinallyBlockExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyBlockEnterNode(fir: FirTryExpression): FinallyBlockEnterNode =
    FinallyBlockEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCatchClauseExitNode(fir: FirCatch): CatchClauseExitNode =
    CatchClauseExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryMainBlockExitNode(fir: FirTryExpression): TryMainBlockExitNode =
    TryMainBlockExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryMainBlockEnterNode(fir: FirTryExpression): TryMainBlockEnterNode =
    TryMainBlockEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCatchClauseEnterNode(fir: FirCatch): CatchClauseEnterNode =
    CatchClauseEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryExpressionEnterNode(fir: FirTryExpression): TryExpressionEnterNode =
    TryExpressionEnterNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryExpressionExitNode(fir: FirTryExpression): TryExpressionExitNode =
    TryExpressionExitNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryAndExitLeftOperandNode =
    BinaryAndExitLeftOperandNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryAndEnterRightOperandNode =
    BinaryAndEnterRightOperandNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryOrEnterRightOperandNode =
    BinaryOrEnterRightOperandNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitSafeCallNode(fir: FirQualifiedAccess): ExitSafeCallNode =
    ExitSafeCallNode(graph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterSafeCallNode(fir: FirQualifiedAccess): EnterSafeCallNode =
    EnterSafeCallNode(graph, fir, levelCounter)