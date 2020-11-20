/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*

fun ControlFlowGraphBuilder.createStubNode(): StubNode = StubNode(currentGraph, levelCounter, createId())

fun ControlFlowGraphBuilder.createContractDescriptionEnterNode(): ContractDescriptionEnterNode =
    ContractDescriptionEnterNode(currentGraph, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopExitNode(fir: FirLoop): LoopExitNode = LoopExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopEnterNode(fir: FirLoop): LoopEnterNode = LoopEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createInitBlockEnterNode(fir: FirAnonymousInitializer): InitBlockEnterNode =
    InitBlockEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createInitBlockExitNode(fir: FirAnonymousInitializer): InitBlockExitNode =
    InitBlockExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTypeOperatorCallNode(fir: FirTypeOperatorCall): TypeOperatorCallNode =
    TypeOperatorCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createEqualityOperatorCallNode(fir: FirEqualityOperatorCall): EqualityOperatorCallNode =
    EqualityOperatorCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchConditionExitNode(fir: FirWhenBranch): WhenBranchConditionExitNode =
    WhenBranchConditionExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createJumpNode(fir: FirJump<*>): JumpNode =
    JumpNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createCheckNotNullCallNode(fir: FirCheckNotNullCall): CheckNotNullCallNode =
    CheckNotNullCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createQualifiedAccessNode(fir: FirQualifiedAccessExpression): QualifiedAccessNode =
    QualifiedAccessNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createResolvedQualifierNode(fir: FirResolvedQualifier): ResolvedQualifierNode =
    ResolvedQualifierNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBlockEnterNode(fir: FirBlock): BlockEnterNode = BlockEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBlockExitNode(fir: FirBlock): BlockExitNode = BlockExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPartOfClassInitializationNode(fir: FirControlFlowGraphOwner): PartOfClassInitializationNode =
    PartOfClassInitializationNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPropertyInitializerExitNode(fir: FirProperty): PropertyInitializerExitNode =
    PropertyInitializerExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPropertyInitializerEnterNode(fir: FirProperty): PropertyInitializerEnterNode =
    PropertyInitializerEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFunctionEnterNode(fir: FirFunction<*>): FunctionEnterNode =
    FunctionEnterNode(currentGraph, fir, levelCounter, createId()).also {
        currentGraph.enterNode = it
    }

fun ControlFlowGraphBuilder.createFunctionExitNode(fir: FirFunction<*>): FunctionExitNode =
    FunctionExitNode(currentGraph, fir, levelCounter, createId()).also {
        currentGraph.exitNode = it
    }

fun ControlFlowGraphBuilder.createLocalFunctionDeclarationNode(fir: FirFunction<*>): LocalFunctionDeclarationNode =
    LocalFunctionDeclarationNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrEnterNode(fir: FirBinaryLogicExpression): BinaryOrEnterNode =
    BinaryOrEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryOrExitLeftOperandNode =
    BinaryOrExitLeftOperandNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrExitNode(fir: FirBinaryLogicExpression): BinaryOrExitNode =
    BinaryOrExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndExitNode(fir: FirBinaryLogicExpression): BinaryAndExitNode =
    BinaryAndExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndEnterNode(fir: FirBinaryLogicExpression): BinaryAndEnterNode =
    BinaryAndEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchConditionEnterNode(fir: FirWhenBranch): WhenBranchConditionEnterNode =
    WhenBranchConditionEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenEnterNode(fir: FirWhenExpression): WhenEnterNode =
    WhenEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenExitNode(fir: FirWhenExpression): WhenExitNode =
    WhenExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchResultExitNode(fir: FirWhenBranch): WhenBranchResultExitNode =
    WhenBranchResultExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenSyntheticElseBranchNode(fir: FirWhenExpression): WhenSyntheticElseBranchNode =
    WhenSyntheticElseBranchNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createWhenBranchResultEnterNode(fir: FirWhenBranch): WhenBranchResultEnterNode =
    WhenBranchResultEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopConditionExitNode(fir: FirExpression): LoopConditionExitNode =
    LoopConditionExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopConditionEnterNode(fir: FirExpression): LoopConditionEnterNode =
    LoopConditionEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopBlockEnterNode(fir: FirLoop): LoopBlockEnterNode =
    LoopBlockEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLoopBlockExitNode(fir: FirLoop): LoopBlockExitNode =
    LoopBlockExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFunctionCallNode(fir: FirFunctionCall): FunctionCallNode =
    FunctionCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createDelegatedConstructorCallNode(fir: FirDelegatedConstructorCall): DelegatedConstructorCallNode =
    DelegatedConstructorCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createVariableAssignmentNode(fir: FirVariableAssignment): VariableAssignmentNode =
    VariableAssignmentNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createAnnotationExitNode(fir: FirAnnotationCall): AnnotationExitNode =
    AnnotationExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createAnnotationEnterNode(fir: FirAnnotationCall): AnnotationEnterNode =
    AnnotationEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createElvisLhsIsNotNullNode(fir: FirElvisExpression): ElvisLhsIsNotNullNode =
    ElvisLhsIsNotNullNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createElvisRhsEnterNode(fir: FirElvisExpression): ElvisRhsEnterNode =
    ElvisRhsEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createElvisLhsExitNode(fir: FirElvisExpression): ElvisLhsExitNode =
    ElvisLhsExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createElvisExitNode(fir: FirElvisExpression): ElvisExitNode =
    ElvisExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createVariableDeclarationNode(fir: FirProperty): VariableDeclarationNode =
    VariableDeclarationNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createExitContractNode(fir: FirQualifiedAccess): ExitContractNode =
    ExitContractNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createEnterContractNode(fir: FirQualifiedAccess): EnterContractNode =
    EnterContractNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createConstExpressionNode(fir: FirConstExpression<*>): ConstExpressionNode =
    ConstExpressionNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createThrowExceptionNode(fir: FirThrowExpression): ThrowExceptionNode =
    ThrowExceptionNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyProxyExitNode(fir: FirTryExpression): FinallyProxyExitNode =
    FinallyProxyExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyProxyEnterNode(fir: FirTryExpression): FinallyProxyEnterNode =
    FinallyProxyEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyBlockExitNode(fir: FirTryExpression): FinallyBlockExitNode =
    FinallyBlockExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createFinallyBlockEnterNode(fir: FirTryExpression): FinallyBlockEnterNode =
    FinallyBlockEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createCatchClauseExitNode(fir: FirCatch): CatchClauseExitNode =
    CatchClauseExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryMainBlockExitNode(fir: FirTryExpression): TryMainBlockExitNode =
    TryMainBlockExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryMainBlockEnterNode(fir: FirTryExpression): TryMainBlockEnterNode =
    TryMainBlockEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createCatchClauseEnterNode(fir: FirCatch): CatchClauseEnterNode =
    CatchClauseEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryExpressionEnterNode(fir: FirTryExpression): TryExpressionEnterNode =
    TryExpressionEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createTryExpressionExitNode(fir: FirTryExpression): TryExpressionExitNode =
    TryExpressionExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryAndExitLeftOperandNode =
    BinaryAndExitLeftOperandNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryAndEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryAndEnterRightOperandNode =
    BinaryAndEnterRightOperandNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createBinaryOrEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryOrEnterRightOperandNode =
    BinaryOrEnterRightOperandNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createExitSafeCallNode(fir: FirSafeCallExpression): ExitSafeCallNode =
    ExitSafeCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createEnterSafeCallNode(fir: FirSafeCallExpression): EnterSafeCallNode =
    EnterSafeCallNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPostponedLambdaExitNode(fir: FirAnonymousFunction): PostponedLambdaExitNode =
    PostponedLambdaExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createPostponedLambdaEnterNode(fir: FirAnonymousFunction): PostponedLambdaEnterNode =
    PostponedLambdaEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createAnonymousObjectExitNode(fir: FirAnonymousObject): AnonymousObjectExitNode =
    AnonymousObjectExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createUnionFunctionCallArgumentsNode(fir: FirElement): UnionFunctionCallArgumentsNode =
    UnionFunctionCallArgumentsNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createClassEnterNode(fir: FirClass<*>): ClassEnterNode =
    ClassEnterNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createClassExitNode(fir: FirClass<*>): ClassExitNode =
    ClassExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createLocalClassExitNode(fir: FirRegularClass): LocalClassExitNode =
    LocalClassExitNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createEnterDefaultArgumentsNode(fir: FirValueParameter): EnterDefaultArgumentsNode =
    EnterDefaultArgumentsNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createExitDefaultArgumentsNode(fir: FirValueParameter): ExitDefaultArgumentsNode =
    ExitDefaultArgumentsNode(currentGraph, fir, levelCounter, createId())

fun ControlFlowGraphBuilder.createComparisonExpressionNode(fir: FirComparisonExpression): ComparisonExpressionNode =
    ComparisonExpressionNode(currentGraph, fir, levelCounter, createId())