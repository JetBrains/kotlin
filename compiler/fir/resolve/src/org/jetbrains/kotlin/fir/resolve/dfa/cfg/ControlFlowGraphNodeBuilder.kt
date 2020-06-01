/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*

fun ControlFlowGraphBuilder.createStubNode(): StubNode = StubNode(graph, levelCounter, createId())

fun ControlFlowGraphBuilder.createContractDescriptionEnterNode(): ContractDescriptionEnterNode =
    ContractDescriptionEnterNode(graph, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopExitNode(fir: FirLoop): LoopExitNode = LoopExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopEnterNode(fir: FirLoop): LoopEnterNode = LoopEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createInitBlockEnterNode(fir: FirAnonymousInitializer): InitBlockEnterNode =
    InitBlockEnterNode(graph, fir, levelCounter, createId()).also {
        graph.enterNode = it
    }

fun ControlFlowGraphBuilder.createInitBlockExitNode(fir: FirAnonymousInitializer): InitBlockExitNode =
    InitBlockExitNode(graph, fir, levelCounter, createId()).also {
        graph.exitNode = it
    }

fun ControlFlowGraphBuilder.createTypeOperatorCallNode(fir: FirTypeOperatorCall): TypeOperatorCallNode =
    TypeOperatorCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createOperatorCallNode(fir: FirOperatorCall): OperatorCallNode =
    OperatorCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchConditionExitNode(fir: FirWhenBranch): WhenBranchConditionExitNode =
    WhenBranchConditionExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createJumpNode(fir: FirJump<*>): JumpNode =
    JumpNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createCheckNotNullCallNode(fir: FirCheckNotNullCall): CheckNotNullCallNode =
    CheckNotNullCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createQualifiedAccessNode(fir: FirQualifiedAccessExpression): QualifiedAccessNode =
    QualifiedAccessNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createResolvedQualifierNode(fir: FirResolvedQualifier): ResolvedQualifierNode =
    ResolvedQualifierNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBlockEnterNode(fir: FirBlock): BlockEnterNode = BlockEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBlockExitNode(fir: FirBlock): BlockExitNode = BlockExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPropertyInitializerExitNode(fir: FirProperty): PropertyInitializerExitNode =
    PropertyInitializerExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPropertyInitializerEnterNode(fir: FirProperty): PropertyInitializerEnterNode =
    PropertyInitializerEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFunctionEnterNode(fir: FirFunction<*>): FunctionEnterNode =
    FunctionEnterNode(graph, fir, levelCounter, createId()).also {
        graph.enterNode = it
    }

fun ControlFlowGraphBuilder.createFunctionExitNode(fir: FirFunction<*>): FunctionExitNode =
    FunctionExitNode(graph, fir, levelCounter, createId()).also {
        graph.exitNode = it
    }

fun ControlFlowGraphBuilder.createBinaryOrEnterNode(fir: FirBinaryLogicExpression): BinaryOrEnterNode =
    BinaryOrEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryOrExitLeftOperandNode =
    BinaryOrExitLeftOperandNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrExitNode(fir: FirBinaryLogicExpression): BinaryOrExitNode =
    BinaryOrExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndExitNode(fir: FirBinaryLogicExpression): BinaryAndExitNode =
    BinaryAndExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndEnterNode(fir: FirBinaryLogicExpression): BinaryAndEnterNode =
    BinaryAndEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchConditionEnterNode(fir: FirWhenBranch): WhenBranchConditionEnterNode =
    WhenBranchConditionEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenEnterNode(fir: FirWhenExpression): WhenEnterNode =
    WhenEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenExitNode(fir: FirWhenExpression): WhenExitNode =
    WhenExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchResultExitNode(fir: FirWhenBranch): WhenBranchResultExitNode =
    WhenBranchResultExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenSyntheticElseBranchNode(fir: FirWhenExpression): WhenSyntheticElseBranchNode =
    WhenSyntheticElseBranchNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchResultEnterNode(fir: FirWhenBranch): WhenBranchResultEnterNode =
    WhenBranchResultEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopConditionExitNode(fir: FirExpression): LoopConditionExitNode =
    LoopConditionExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopConditionEnterNode(fir: FirExpression): LoopConditionEnterNode =
    LoopConditionEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopBlockEnterNode(fir: FirLoop): LoopBlockEnterNode =
    LoopBlockEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopBlockExitNode(fir: FirLoop): LoopBlockExitNode =
    LoopBlockExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFunctionCallNode(fir: FirFunctionCall): FunctionCallNode =
    FunctionCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createDelegatedConstructorCallNode(fir: FirDelegatedConstructorCall): DelegatedConstructorCallNode =
    DelegatedConstructorCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createVariableAssignmentNode(fir: FirVariableAssignment): VariableAssignmentNode =
    VariableAssignmentNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createAnnotationExitNode(fir: FirAnnotationCall): AnnotationExitNode =
    AnnotationExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createAnnotationEnterNode(fir: FirAnnotationCall): AnnotationEnterNode =
    AnnotationEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createVariableDeclarationNode(fir: FirProperty): VariableDeclarationNode =
    VariableDeclarationNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createExitContractNode(fir: FirQualifiedAccess): ExitContractNode =
    ExitContractNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createEnterContractNode(fir: FirQualifiedAccess): EnterContractNode =
    EnterContractNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createConstExpressionNode(fir: FirConstExpression<*>): ConstExpressionNode =
    ConstExpressionNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createThrowExceptionNode(fir: FirThrowExpression): ThrowExceptionNode =
    ThrowExceptionNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyProxyExitNode(fir: FirTryExpression): FinallyProxyExitNode =
    FinallyProxyExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyProxyEnterNode(fir: FirTryExpression): FinallyProxyEnterNode =
    FinallyProxyEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyBlockExitNode(fir: FirTryExpression): FinallyBlockExitNode =
    FinallyBlockExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyBlockEnterNode(fir: FirTryExpression): FinallyBlockEnterNode =
    FinallyBlockEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createCatchClauseExitNode(fir: FirCatch): CatchClauseExitNode =
    CatchClauseExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryMainBlockExitNode(fir: FirTryExpression): TryMainBlockExitNode =
    TryMainBlockExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryMainBlockEnterNode(fir: FirTryExpression): TryMainBlockEnterNode =
    TryMainBlockEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createCatchClauseEnterNode(fir: FirCatch): CatchClauseEnterNode =
    CatchClauseEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryExpressionEnterNode(fir: FirTryExpression): TryExpressionEnterNode =
    TryExpressionEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryExpressionExitNode(fir: FirTryExpression): TryExpressionExitNode =
    TryExpressionExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryAndExitLeftOperandNode =
    BinaryAndExitLeftOperandNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryAndEnterRightOperandNode =
    BinaryAndEnterRightOperandNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryOrEnterRightOperandNode =
    BinaryOrEnterRightOperandNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createExitSafeCallNode(fir: FirSafeCallExpression): ExitSafeCallNode =
    ExitSafeCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createEnterSafeCallNode(fir: FirSafeCallExpression): EnterSafeCallNode =
    EnterSafeCallNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPostponedLambdaExitNode(fir: FirAnonymousFunction): PostponedLambdaExitNode =
    PostponedLambdaExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPostponedLambdaEnterNode(fir: FirAnonymousFunction): PostponedLambdaEnterNode =
    PostponedLambdaEnterNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createAnonymousObjectExitNode(fir: FirAnonymousObject): AnonymousObjectExitNode =
    AnonymousObjectExitNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createUnionFunctionCallArgumentsNode(fir: FirElement): UnionFunctionCallArgumentsNode =
    UnionFunctionCallArgumentsNode(graph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createClassEnterNode(fir: FirClass<*>): ClassEnterNode =
    ClassEnterNode(graph, fir, levelCounter, createId()).also {
        graph.enterNode = it
    }

fun ControlFlowGraphBuilder.createClassExitNode(fir: FirClass<*>): ClassExitNode =
    ClassExitNode(graph, fir, levelCounter, createId()).also {
        graph.exitNode = it
    }

fun ControlFlowGraphBuilder.createLocalClassExitNode(fir: FirRegularClass): LocalClassExitNode =
    LocalClassExitNode(graph, fir, levelCounter, createId())
