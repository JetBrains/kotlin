/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*

fun ControlFlowGraphBuilder.createStubNode(): StubNode = StubNode(currentGraph, levelCounter)

fun ControlFlowGraphBuilder.createFakeExpressionEnterNode(): FakeExpressionEnterNode =
    FakeExpressionEnterNode(currentGraph, levelCounter)

fun ControlFlowGraphBuilder.createLoopExitNode(fir: FirLoop): LoopExitNode = LoopExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopEnterNode(fir: FirLoop): LoopEnterNode = LoopEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createInitBlockEnterNode(fir: FirAnonymousInitializer): InitBlockEnterNode =
    InitBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createInitBlockExitNode(fir: FirAnonymousInitializer): InitBlockExitNode =
    InitBlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTypeOperatorCallNode(fir: FirTypeOperatorCall): TypeOperatorCallNode =
    TypeOperatorCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEqualityOperatorCallNode(fir: FirEqualityOperatorCall): EqualityOperatorCallNode =
    EqualityOperatorCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchConditionExitNode(fir: FirWhenBranch): WhenBranchConditionExitNode =
    WhenBranchConditionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createJumpNode(fir: FirJump<*>): JumpNode =
    JumpNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCheckNotNullCallNode(fir: FirCheckNotNullCall): CheckNotNullCallNode =
    CheckNotNullCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createQualifiedAccessNode(fir: FirQualifiedAccessExpression): QualifiedAccessNode =
    QualifiedAccessNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createResolvedQualifierNode(fir: FirResolvedQualifier): ResolvedQualifierNode =
    ResolvedQualifierNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBlockEnterNode(fir: FirBlock): BlockEnterNode = BlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBlockExitNode(fir: FirBlock): BlockExitNode = BlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createPropertyInitializerExitNode(fir: FirProperty): PropertyInitializerExitNode =
    PropertyInitializerExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createPropertyInitializerEnterNode(fir: FirProperty): PropertyInitializerEnterNode =
    PropertyInitializerEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createDelegateExpressionExitNode(fir: FirExpression): DelegateExpressionExitNode =
    DelegateExpressionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFieldInitializerExitNode(fir: FirField): FieldInitializerExitNode =
    FieldInitializerExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFieldInitializerEnterNode(fir: FirField): FieldInitializerEnterNode =
    FieldInitializerEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionEnterNode(fir: FirFunction): FunctionEnterNode =
    FunctionEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionExitNode(fir: FirFunction): FunctionExitNode =
    FunctionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLocalFunctionDeclarationNode(fir: FirFunction): LocalFunctionDeclarationNode =
    LocalFunctionDeclarationNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrEnterNode(fir: FirBinaryLogicExpression): BinaryOrEnterNode =
    BinaryOrEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryOrExitLeftOperandNode =
    BinaryOrExitLeftOperandNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrExitNode(fir: FirBinaryLogicExpression): BinaryOrExitNode =
    BinaryOrExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndExitNode(fir: FirBinaryLogicExpression): BinaryAndExitNode =
    BinaryAndExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndEnterNode(fir: FirBinaryLogicExpression): BinaryAndEnterNode =
    BinaryAndEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchConditionEnterNode(fir: FirWhenBranch): WhenBranchConditionEnterNode =
    WhenBranchConditionEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenEnterNode(fir: FirWhenExpression): WhenEnterNode =
    WhenEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenExitNode(fir: FirWhenExpression): WhenExitNode =
    WhenExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchResultExitNode(fir: FirWhenBranch): WhenBranchResultExitNode =
    WhenBranchResultExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenSyntheticElseBranchNode(fir: FirWhenExpression): WhenSyntheticElseBranchNode =
    WhenSyntheticElseBranchNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenBranchResultEnterNode(fir: FirWhenBranch): WhenBranchResultEnterNode =
    WhenBranchResultEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopConditionExitNode(fir: FirExpression): LoopConditionExitNode =
    LoopConditionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopConditionEnterNode(fir: FirExpression, loop: FirLoop): LoopConditionEnterNode =
    LoopConditionEnterNode(currentGraph, fir, loop, levelCounter)

fun ControlFlowGraphBuilder.createLoopBlockEnterNode(fir: FirLoop): LoopBlockEnterNode =
    LoopBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopBlockExitNode(fir: FirLoop): LoopBlockExitNode =
    LoopBlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionCallNode(fir: FirFunctionCall): FunctionCallNode =
    FunctionCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCallableReferenceNode(fir: FirCallableReferenceAccess): CallableReferenceNode =
    CallableReferenceNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createGetClassCallNode(fir: FirGetClassCall): GetClassCallNode =
    GetClassCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createDelegatedConstructorCallNode(fir: FirDelegatedConstructorCall): DelegatedConstructorCallNode =
    DelegatedConstructorCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createStringConcatenationCallNode(fir: FirStringConcatenationCall): StringConcatenationCallNode =
    StringConcatenationCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createVariableAssignmentNode(fir: FirVariableAssignment): VariableAssignmentNode =
    VariableAssignmentNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createElvisLhsIsNotNullNode(fir: FirElvisExpression): ElvisLhsIsNotNullNode =
    ElvisLhsIsNotNullNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createElvisRhsEnterNode(fir: FirElvisExpression): ElvisRhsEnterNode =
    ElvisRhsEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createElvisLhsExitNode(fir: FirElvisExpression): ElvisLhsExitNode =
    ElvisLhsExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createWhenSubjectExpressionExitNode(fir: FirWhenSubjectExpression): WhenSubjectExpressionExitNode =
    WhenSubjectExpressionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createElvisExitNode(fir: FirElvisExpression): ElvisExitNode =
    ElvisExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createVariableDeclarationNode(fir: FirProperty): VariableDeclarationNode =
    VariableDeclarationNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createConstExpressionNode(fir: FirConstExpression<*>): ConstExpressionNode =
    ConstExpressionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createThrowExceptionNode(fir: FirThrowExpression): ThrowExceptionNode =
    ThrowExceptionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyBlockExitNode(fir: FirTryExpression): FinallyBlockExitNode =
    FinallyBlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyBlockEnterNode(fir: FirTryExpression): FinallyBlockEnterNode =
    FinallyBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCatchClauseExitNode(fir: FirCatch): CatchClauseExitNode =
    CatchClauseExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryMainBlockExitNode(fir: FirTryExpression): TryMainBlockExitNode =
    TryMainBlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryMainBlockEnterNode(fir: FirTryExpression): TryMainBlockEnterNode =
    TryMainBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCatchClauseEnterNode(fir: FirCatch): CatchClauseEnterNode =
    CatchClauseEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryExpressionEnterNode(fir: FirTryExpression): TryExpressionEnterNode =
    TryExpressionEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryExpressionExitNode(fir: FirTryExpression): TryExpressionExitNode =
    TryExpressionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndExitLeftOperandNode(fir: FirBinaryLogicExpression): BinaryAndExitLeftOperandNode =
    BinaryAndExitLeftOperandNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryAndEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryAndEnterRightOperandNode =
    BinaryAndEnterRightOperandNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBinaryOrEnterRightOperandNode(fir: FirBinaryLogicExpression): BinaryOrEnterRightOperandNode =
    BinaryOrEnterRightOperandNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitSafeCallNode(fir: FirSafeCallExpression): ExitSafeCallNode =
    ExitSafeCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterSafeCallNode(fir: FirSafeCallExpression): EnterSafeCallNode =
    EnterSafeCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createPostponedLambdaExitNode(fir: FirAnonymousFunctionExpression): PostponedLambdaExitNode =
    PostponedLambdaExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createSplitPostponedLambdasNode(fir: FirStatement, lambdas: List<FirAnonymousFunction>): SplitPostponedLambdasNode =
    SplitPostponedLambdasNode(currentGraph, fir, lambdas, levelCounter)

fun ControlFlowGraphBuilder.createMergePostponedLambdaExitsNode(fir: FirElement): MergePostponedLambdaExitsNode =
    MergePostponedLambdaExitsNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createAnonymousFunctionExpressionNode(fir: FirAnonymousFunctionExpression): AnonymousFunctionExpressionNode =
    AnonymousFunctionExpressionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createAnonymousObjectEnterNode(fir: FirAnonymousObject): AnonymousObjectEnterNode =
    AnonymousObjectEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createAnonymousObjectExpressionExitNode(fir: FirAnonymousObjectExpression): AnonymousObjectExpressionExitNode =
    AnonymousObjectExpressionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createScriptEnterNode(fir: FirScript): ScriptEnterNode =
    ScriptEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createScriptExitNode(fir: FirScript): ScriptExitNode =
    ScriptExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCodeFragmentEnterNode(fir: FirCodeFragment): CodeFragmentEnterNode =
    CodeFragmentEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCodeFragmentExitNode(fir: FirCodeFragment): CodeFragmentExitNode =
    CodeFragmentExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFileEnterNode(fir: FirFile): FileEnterNode =
    FileEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFileExitNode(fir: FirFile): FileExitNode =
    FileExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createClassEnterNode(fir: FirClass): ClassEnterNode =
    ClassEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createClassExitNode(fir: FirClass): ClassExitNode =
    ClassExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLocalClassExitNode(fir: FirRegularClass): LocalClassExitNode =
    LocalClassExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterValueParameterNode(fir: FirValueParameter): EnterValueParameterNode =
    EnterValueParameterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterDefaultArgumentsNode(fir: FirValueParameter): EnterDefaultArgumentsNode =
    EnterDefaultArgumentsNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitDefaultArgumentsNode(fir: FirValueParameter): ExitDefaultArgumentsNode =
    ExitDefaultArgumentsNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitValueParameterNode(fir: FirValueParameter): ExitValueParameterNode =
    ExitValueParameterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createComparisonExpressionNode(fir: FirComparisonExpression): ComparisonExpressionNode =
    ComparisonExpressionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createSmartCastExitNode(fir: FirSmartCastExpression): SmartCastExpressionExitNode =
    SmartCastExpressionExitNode(currentGraph, fir, levelCounter)