/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

abstract class ControlFlowGraphVisitorVoid : ControlFlowGraphVisitor<Unit, Nothing?>() {
    abstract fun visitNode(node: CFGNode<*>)

    // ----------------------------------- Simple function -----------------------------------

    open fun visitFunctionEnterNode(node: FunctionEnterNode) {
        visitNode(node)
    }

    open fun visitFunctionExitNode(node: FunctionExitNode) {
        visitNode(node)
    }

    open fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode) {
        visitNode(node)
    }

    // ----------------------------------- Anonymous function -----------------------------------

    open fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode) {
        visitNode(node)
    }

    open fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode) {
        visitNode(node)
    }

    open fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode) {
        visitNode(node)
    }

    open fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode) {
        visitNode(node)
    }

    // ----------------------------------- Property -----------------------------------

    open fun visitPropertyInitializerEnterNode(node: PropertyInitializerEnterNode) {
        visitNode(node)
    }

    open fun visitPropertyInitializerExitNode(node: PropertyInitializerExitNode) {
        visitNode(node)
    }

    open fun visitDelegateExpressionExitNode(node: DelegateExpressionExitNode) {
        visitNode(node)
    }

    // ----------------------------------- Init -----------------------------------

    open fun visitInitBlockEnterNode(node: InitBlockEnterNode) {
        visitNode(node)
    }

    open fun visitInitBlockExitNode(node: InitBlockExitNode) {
        visitNode(node)
    }

    // ----------------------------------- Block -----------------------------------

    open fun visitBlockEnterNode(node: BlockEnterNode) {
        visitNode(node)
    }

    open fun visitBlockExitNode(node: BlockExitNode) {
        visitNode(node)
    }

    // ----------------------------------- When -----------------------------------

    open fun visitWhenEnterNode(node: WhenEnterNode) {
        visitNode(node)
    }

    open fun visitWhenExitNode(node: WhenExitNode) {
        visitNode(node)
    }

    open fun visitWhenBranchConditionEnterNode(node: WhenBranchConditionEnterNode) {
        visitNode(node)
    }

    open fun visitWhenBranchConditionExitNode(node: WhenBranchConditionExitNode) {
        visitNode(node)
    }

    open fun visitWhenBranchResultEnterNode(node: WhenBranchResultEnterNode) {
        visitNode(node)
    }

    open fun visitWhenBranchResultExitNode(node: WhenBranchResultExitNode) {
        visitNode(node)
    }

    open fun visitWhenSyntheticElseBranchNode(node: WhenSyntheticElseBranchNode) {
        visitNode(node)
    }


    // ----------------------------------- Loop -----------------------------------

    open fun visitLoopEnterNode(node: LoopEnterNode) {
        visitNode(node)
    }

    open fun visitLoopBlockEnterNode(node: LoopBlockEnterNode) {
        visitNode(node)
    }

    open fun visitLoopBlockExitNode(node: LoopBlockExitNode) {
        visitNode(node)
    }

    open fun visitLoopConditionEnterNode(node: LoopConditionEnterNode) {
        visitNode(node)
    }

    open fun visitLoopConditionExitNode(node: LoopConditionExitNode) {
        visitNode(node)
    }

    open fun visitLoopExitNode(node: LoopExitNode) {
        visitNode(node)
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    open fun visitTryExpressionEnterNode(node: TryExpressionEnterNode) {
        visitNode(node)
    }

    open fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode) {
        visitNode(node)
    }

    open fun visitTryMainBlockExitNode(node: TryMainBlockExitNode) {
        visitNode(node)
    }

    open fun visitCatchClauseEnterNode(node: CatchClauseEnterNode) {
        visitNode(node)
    }

    open fun visitCatchClauseExitNode(node: CatchClauseExitNode) {
        visitNode(node)
    }

    open fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode) {
        visitNode(node)
    }

    open fun visitFinallyBlockExitNode(node: FinallyBlockExitNode) {
        visitNode(node)
    }

    open fun visitTryExpressionExitNode(node: TryExpressionExitNode) {
        visitNode(node)
    }

    // ----------------------------------- Boolean operators -----------------------------------

    open fun visitBinaryAndEnterNode(node: BinaryAndEnterNode) {
        visitNode(node)
    }

    open fun visitBinaryAndExitLeftOperandNode(node: BinaryAndExitLeftOperandNode) {
        visitNode(node)
    }

    open fun visitBinaryAndEnterRightOperandNode(node: BinaryAndEnterRightOperandNode) {
        visitNode(node)
    }

    open fun visitBinaryAndExitNode(node: BinaryAndExitNode) {
        visitNode(node)
    }

    open fun visitBinaryOrEnterNode(node: BinaryOrEnterNode) {
        visitNode(node)
    }

    open fun visitBinaryOrExitLeftOperandNode(node: BinaryOrExitLeftOperandNode) {
        visitNode(node)
    }

    open fun visitBinaryOrEnterRightOperandNode(node: BinaryOrEnterRightOperandNode) {
        visitNode(node)
    }

    open fun visitBinaryOrExitNode(node: BinaryOrExitNode) {
        visitNode(node)
    }

    // ----------------------------------- Operator call -----------------------------------

    open fun visitTypeOperatorCallNode(node: TypeOperatorCallNode) {
        visitNode(node)
    }

    open fun visitComparisonExpressionNode(node: ComparisonExpressionNode) {
        visitNode(node)
    }

    open fun visitEqualityOperatorCallNode(node: EqualityOperatorCallNode) {
        visitNode(node)
    }

    // ----------------------------------- Jump -----------------------------------

    open fun visitJumpNode(node: JumpNode) {
        visitNode(node)
    }

    open fun visitConstExpressionNode(node: ConstExpressionNode) {
        visitNode(node)
    }

    // ----------------------------------- Check not null call -----------------------------------

    open fun visitCheckNotNullCallNode(node: CheckNotNullCallNode) {
        visitNode(node)
    }

    // ----------------------------------- Resolvable call -----------------------------------

    open fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
        visitNode(node)
    }

    open fun visitResolvedQualifierNode(node: ResolvedQualifierNode) {
        visitNode(node)
    }

    open fun visitFunctionCallNode(node: FunctionCallNode) {
        visitNode(node)
    }

    open fun visitDelegatedConstructorCallNode(node: DelegatedConstructorCallNode) {
        visitNode(node)
    }

    open fun visitStringConcatenationCallNode(node: StringConcatenationCallNode) {
        visitNode(node)
    }

    open fun visitThrowExceptionNode(node: ThrowExceptionNode) {
        visitNode(node)
    }

    open fun visitStubNode(node: StubNode) {
        visitNode(node)
    }

    open fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
        visitNode(node)
    }

    open fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
        visitNode(node)
    }

    open fun visitEnterSafeCallNode(node: EnterSafeCallNode) {
        visitNode(node)
    }

    open fun visitExitSafeCallNode(node: ExitSafeCallNode) {
        visitNode(node)
    }

    // ---------------------------------------------------------------------------------------------------------------------

    final override fun visitNode(node: CFGNode<*>, data: Nothing?) {
        visitNode(node)
    }

    // ----------------------------------- Simple function -----------------------------------

    final override fun visitFunctionEnterNode(node: FunctionEnterNode, data: Nothing?) {
        visitFunctionEnterNode(node)
    }

    final override fun visitFunctionExitNode(node: FunctionExitNode, data: Nothing?) {
        visitFunctionExitNode(node)
    }

    final override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Nothing?) {
        visitLocalFunctionDeclarationNode(node)
    }

    // ----------------------------------- Anonymous function -----------------------------------

    final override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: Nothing?) {
        visitSplitPostponedLambdasNode(node)
    }

    final override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: Nothing?) {
        visitPostponedLambdaExitNode(node)
    }

    final override fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: Nothing?) {
        visitMergePostponedLambdaExitsNode(node)
    }

    final override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: Nothing?) {
        visitAnonymousFunctionExpressionNode(node)
    }

    // ----------------------------------- Property -----------------------------------

    final override fun visitPropertyInitializerEnterNode(node: PropertyInitializerEnterNode, data: Nothing?) {
        visitPropertyInitializerEnterNode(node)
    }

    final override fun visitPropertyInitializerExitNode(node: PropertyInitializerExitNode, data: Nothing?) {
        visitPropertyInitializerExitNode(node)
    }

    final override fun visitDelegateExpressionExitNode(node: DelegateExpressionExitNode, data: Nothing?) {
        visitDelegateExpressionExitNode(node)
    }

    // ----------------------------------- Init -----------------------------------

    final override fun visitInitBlockEnterNode(node: InitBlockEnterNode, data: Nothing?) {
        visitInitBlockEnterNode(node)
    }

    final override fun visitInitBlockExitNode(node: InitBlockExitNode, data: Nothing?) {
        visitInitBlockExitNode(node)
    }

    // ----------------------------------- Block -----------------------------------

    final override fun visitBlockEnterNode(node: BlockEnterNode, data: Nothing?) {
        visitBlockEnterNode(node)
    }

    final override fun visitBlockExitNode(node: BlockExitNode, data: Nothing?) {
        visitBlockExitNode(node)
    }

    // ----------------------------------- When -----------------------------------

    final override fun visitWhenEnterNode(node: WhenEnterNode, data: Nothing?) {
        visitWhenEnterNode(node)
    }

    final override fun visitWhenExitNode(node: WhenExitNode, data: Nothing?) {
        visitWhenExitNode(node)
    }

    final override fun visitWhenBranchConditionEnterNode(node: WhenBranchConditionEnterNode, data: Nothing?) {
        visitWhenBranchConditionEnterNode(node)
    }

    final override fun visitWhenBranchConditionExitNode(node: WhenBranchConditionExitNode, data: Nothing?) {
        visitWhenBranchConditionExitNode(node)
    }

    final override fun visitWhenBranchResultEnterNode(node: WhenBranchResultEnterNode, data: Nothing?) {
        visitWhenBranchResultEnterNode(node)
    }

    final override fun visitWhenBranchResultExitNode(node: WhenBranchResultExitNode, data: Nothing?) {
        visitWhenBranchResultExitNode(node)
    }

    final override fun visitWhenSyntheticElseBranchNode(node: WhenSyntheticElseBranchNode, data: Nothing?) {
        visitWhenSyntheticElseBranchNode(node)
    }


    // ----------------------------------- Loop -----------------------------------

    final override fun visitLoopEnterNode(node: LoopEnterNode, data: Nothing?) {
        visitLoopEnterNode(node)
    }

    final override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: Nothing?) {
        visitLoopBlockEnterNode(node)
    }

    final override fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: Nothing?) {
        visitLoopBlockExitNode(node)
    }

    final override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: Nothing?) {
        visitLoopConditionEnterNode(node)
    }

    final override fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: Nothing?) {
        visitLoopConditionExitNode(node)
    }

    final override fun visitLoopExitNode(node: LoopExitNode, data: Nothing?) {
        visitLoopExitNode(node)
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    final override fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: Nothing?) {
        visitTryExpressionEnterNode(node)
    }

    final override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: Nothing?) {
        visitTryMainBlockEnterNode(node)
    }

    final override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: Nothing?) {
        visitTryMainBlockExitNode(node)
    }

    final override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: Nothing?) {
        visitCatchClauseEnterNode(node)
    }

    final override fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: Nothing?) {
        visitCatchClauseExitNode(node)
    }

    final override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: Nothing?) {
        visitFinallyBlockEnterNode(node)
    }

    final override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: Nothing?) {
        visitFinallyBlockExitNode(node)
    }

    final override fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: Nothing?) {
        visitTryExpressionExitNode(node)
    }

    // ----------------------------------- Boolean operators -----------------------------------

    final override fun visitBinaryAndEnterNode(node: BinaryAndEnterNode, data: Nothing?) {
        visitBinaryAndEnterNode(node)
    }

    final override fun visitBinaryAndExitLeftOperandNode(node: BinaryAndExitLeftOperandNode, data: Nothing?) {
        visitBinaryAndExitLeftOperandNode(node)
    }

    final override fun visitBinaryAndEnterRightOperandNode(node: BinaryAndEnterRightOperandNode, data: Nothing?) {
        visitBinaryAndEnterRightOperandNode(node)
    }

    final override fun visitBinaryAndExitNode(node: BinaryAndExitNode, data: Nothing?) {
        visitBinaryAndExitNode(node)
    }

    final override fun visitBinaryOrEnterNode(node: BinaryOrEnterNode, data: Nothing?) {
        visitBinaryOrEnterNode(node)
    }

    final override fun visitBinaryOrExitLeftOperandNode(node: BinaryOrExitLeftOperandNode, data: Nothing?) {
        visitBinaryOrExitLeftOperandNode(node)
    }

    final override fun visitBinaryOrEnterRightOperandNode(node: BinaryOrEnterRightOperandNode, data: Nothing?) {
        visitBinaryOrEnterRightOperandNode(node)
    }

    final override fun visitBinaryOrExitNode(node: BinaryOrExitNode, data: Nothing?) {
        visitBinaryOrExitNode(node)
    }

    // ----------------------------------- Operator call -----------------------------------

    final override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: Nothing?) {
        visitTypeOperatorCallNode(node)
    }

    final override fun visitEqualityOperatorCallNode(node: EqualityOperatorCallNode, data: Nothing?) {
        visitEqualityOperatorCallNode(node)
    }

    final override fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: Nothing?) {
        visitComparisonExpressionNode(node)
    }

    // ----------------------------------- Jump -----------------------------------

    final override fun visitJumpNode(node: JumpNode, data: Nothing?) {
        visitJumpNode(node)
    }

    final override fun visitConstExpressionNode(node: ConstExpressionNode, data: Nothing?) {
        visitConstExpressionNode(node)
    }

    // ----------------------------------- Check not null call -----------------------------------

    final override fun visitCheckNotNullCallNode(node: CheckNotNullCallNode, data: Nothing?) {
        visitCheckNotNullCallNode(node)
    }

    // ----------------------------------- Resolvable call -----------------------------------

    final override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Nothing?) {
        visitQualifiedAccessNode(node)
    }

    final override fun visitResolvedQualifierNode(node: ResolvedQualifierNode, data: Nothing?) {
        visitResolvedQualifierNode(node)
    }

    final override fun visitFunctionCallNode(node: FunctionCallNode, data: Nothing?) {
        visitFunctionCallNode(node)
    }

    final override fun visitDelegatedConstructorCallNode(node: DelegatedConstructorCallNode, data: Nothing?) {
        visitDelegatedConstructorCallNode(node)
    }

    final override fun visitStringConcatenationCallNode(node: StringConcatenationCallNode, data: Nothing?) {
        visitStringConcatenationCallNode(node)
    }

    final override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: Nothing?) {
        visitThrowExceptionNode(node)
    }

    final override fun visitStubNode(node: StubNode, data: Nothing?) {
        visitStubNode(node)
    }

    final override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: Nothing?) {
        visitVariableDeclarationNode(node)
    }

    final override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Nothing?) {
        visitVariableAssignmentNode(node)
    }

    final override fun visitEnterSafeCallNode(node: EnterSafeCallNode, data: Nothing?) {
        visitEnterSafeCallNode(node)
    }

    final override fun visitExitSafeCallNode(node: ExitSafeCallNode, data: Nothing?) {
        visitExitSafeCallNode(node)
    }
}
