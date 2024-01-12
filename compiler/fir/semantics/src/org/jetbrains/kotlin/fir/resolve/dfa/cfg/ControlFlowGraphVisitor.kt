/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

abstract class ControlFlowGraphVisitor<out R, in D> {
    abstract fun visitNode(node: CFGNode<*>, data: D): R

    // ----------------------------------- Simple function -----------------------------------

    open fun visitFunctionEnterNode(node: FunctionEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFunctionExitNode(node: FunctionExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Default arguments -----------------------------------

    open fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitExitValueParameterNode(node: ExitValueParameterNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Anonymous function -----------------------------------

    open fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: D): R {
        return visitNode(node, data)
    }

     // ----------------------------------- Files ------------------------------------------

    open fun visitFileEnterNode(node: FileEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFileExitNode(node: FileExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Classes -----------------------------------

    open fun visitAnonymousObjectEnterNode(node: AnonymousObjectEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitAnonymousObjectExpressionExitNode(node: AnonymousObjectExpressionExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitClassEnterNode(node: ClassEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitClassExitNode(node: ClassExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLocalClassExitNode(node: LocalClassExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Scripts ------------------------------------------

    open fun visitScriptEnterNode(node: ScriptEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitScriptExitNode(node: ScriptExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Code Fragments ------------------------------------------

    open fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Property -----------------------------------

    open fun visitPropertyInitializerEnterNode(node: PropertyInitializerEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitPropertyInitializerExitNode(node: PropertyInitializerExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitDelegateExpressionExitNode(node: DelegateExpressionExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Field -----------------------------------

    open fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Init -----------------------------------

    open fun visitInitBlockEnterNode(node: InitBlockEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitInitBlockExitNode(node: InitBlockExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Block -----------------------------------

    open fun visitBlockEnterNode(node: BlockEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBlockExitNode(node: BlockExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- When -----------------------------------

    open fun visitWhenEnterNode(node: WhenEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenExitNode(node: WhenExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenBranchConditionEnterNode(node: WhenBranchConditionEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenBranchConditionExitNode(node: WhenBranchConditionExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenBranchResultEnterNode(node: WhenBranchResultEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenBranchResultExitNode(node: WhenBranchResultExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenSyntheticElseBranchNode(node: WhenSyntheticElseBranchNode, data: D): R {
        return visitNode(node, data)
    }


    // ----------------------------------- Loop -----------------------------------

    open fun visitLoopEnterNode(node: LoopEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLoopExitNode(node: LoopExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    open fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Boolean operators -----------------------------------

    open fun visitBinaryAndEnterNode(node: BinaryAndEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryAndExitLeftOperandNode(node: BinaryAndExitLeftOperandNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryAndEnterRightOperandNode(node: BinaryAndEnterRightOperandNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryAndExitNode(node: BinaryAndExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryOrEnterNode(node: BinaryOrEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryOrExitLeftOperandNode(node: BinaryOrExitLeftOperandNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryOrEnterRightOperandNode(node: BinaryOrEnterRightOperandNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitBinaryOrExitNode(node: BinaryOrExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Operator call -----------------------------------

    open fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitEqualityOperatorCallNode(node: EqualityOperatorCallNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Jump -----------------------------------

    open fun visitJumpNode(node: JumpNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Check not null call -----------------------------------

    open fun visitCheckNotNullCallNode(node: CheckNotNullCallNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Resolvable call -----------------------------------

    open fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitResolvedQualifierNode(node: ResolvedQualifierNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitFunctionCallNode(node: FunctionCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitCallableReferenceNode(node: CallableReferenceNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitGetClassCallNode(node: GetClassCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitDelegatedConstructorCallNode(node: DelegatedConstructorCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitStringConcatenationCallNode(node: StringConcatenationCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitThrowExceptionNode(node: ThrowExceptionNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitStubNode(node: StubNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitEnterSafeCallNode(node: EnterSafeCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitExitSafeCallNode(node: ExitSafeCallNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitWhenSubjectExpressionExitNode(node: WhenSubjectExpressionExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Elvis -----------------------------------

    open fun visitElvisLhsExitNode(node: ElvisLhsExitNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitElvisLhsIsNotNullNode(node: ElvisLhsIsNotNullNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitElvisRhsEnterNode(node: ElvisRhsEnterNode, data: D): R {
        return visitNode(node, data)
    }

    open fun visitElvisExitNode(node: ElvisExitNode, data: D): R {
        return visitNode(node, data)
    }

    // ----------------------------------- Other -----------------------------------

    open fun visitSmartCastExpressionExitNode(node: SmartCastExpressionExitNode, data: D): R {
        return visitNode(node, data)
    }
}
