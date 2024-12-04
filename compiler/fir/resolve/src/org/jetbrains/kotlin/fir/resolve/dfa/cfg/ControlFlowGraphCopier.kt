/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement

internal class ControlFlowGraphCopier : ControlFlowGraphVisitor<CFGNode<*>, Unit>() {
    private val cachedNodes = HashMap<CFGNode<*>, CFGNode<*>>()

    operator fun <E : FirElement, N : CFGNode<E>> get(node: N): N {
        @Suppress("UNCHECKED_CAST")
        return cachedNodes.computeIfAbsent(node) { node -> node.accept(this, Unit) } as N
    }

    override fun visitNode(node: CFGNode<*>, data: Unit): CFGNode<*> {
        error("Copying is not implemented for ${node::class.simpleName}")
    }

    override fun visitFunctionEnterNode(node: FunctionEnterNode, data: Unit): CFGNode<*> {
        return FunctionEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFunctionExitNode(node: FunctionExitNode, data: Unit): CFGNode<*> {
        return FunctionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Unit): CFGNode<*> {
        return LocalFunctionDeclarationNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: Unit): CFGNode<*> {
        return EnterValueParameterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: Unit): CFGNode<*> {
        return EnterDefaultArgumentsNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: Unit): CFGNode<*> {
        return ExitDefaultArgumentsNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitExitValueParameterNode(node: ExitValueParameterNode, data: Unit): CFGNode<*> {
        return ExitValueParameterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: Unit): CFGNode<*> {
        return SplitPostponedLambdasNode(node.owner, node.fir, node.lambdas, node.level).withData(node)
    }

    override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: Unit): CFGNode<*> {
        return PostponedLambdaExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: Unit): CFGNode<*> {
        return MergePostponedLambdaExitsNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: Unit): CFGNode<*> {
        return AnonymousFunctionCaptureNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: Unit): CFGNode<*> {
        return AnonymousFunctionExpressionNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFileEnterNode(node: FileEnterNode, data: Unit): CFGNode<*> {
        return FileEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFileExitNode(node: FileExitNode, data: Unit): CFGNode<*> {
        return FileExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitAnonymousObjectEnterNode(node: AnonymousObjectEnterNode, data: Unit): CFGNode<*> {
        return AnonymousObjectEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitAnonymousObjectExpressionExitNode(node: AnonymousObjectExpressionExitNode, data: Unit): CFGNode<*> {
        return AnonymousObjectExpressionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitClassEnterNode(node: ClassEnterNode, data: Unit): CFGNode<*> {
        return ClassEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitClassExitNode(node: ClassExitNode, data: Unit): CFGNode<*> {
        return ClassExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLocalClassExitNode(node: LocalClassExitNode, data: Unit): CFGNode<*> {
        return LocalClassExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitScriptEnterNode(node: ScriptEnterNode, data: Unit): CFGNode<*> {
        return ScriptEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitScriptExitNode(node: ScriptExitNode, data: Unit): CFGNode<*> {
        return ScriptExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: Unit): CFGNode<*> {
        return CodeFragmentEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: Unit): CFGNode<*> {
        return CodeFragmentExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitReplSnippetEnterNode(node: ReplSnippetEnterNode, data: Unit): CFGNode<*> {
        return ReplSnippetEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitReplSnippetExitNode(node: ReplSnippetExitNode, data: Unit): CFGNode<*> {
        return ReplSnippetExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitPropertyInitializerEnterNode(node: PropertyInitializerEnterNode, data: Unit): CFGNode<*> {
        return PropertyInitializerEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitPropertyInitializerExitNode(node: PropertyInitializerExitNode, data: Unit): CFGNode<*> {
        return PropertyInitializerExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitDelegateExpressionExitNode(node: DelegateExpressionExitNode, data: Unit): CFGNode<*> {
        return DelegateExpressionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: Unit): CFGNode<*> {
        return FieldInitializerEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: Unit): CFGNode<*> {
        return FieldInitializerExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitInitBlockEnterNode(node: InitBlockEnterNode, data: Unit): CFGNode<*> {
        return InitBlockEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitInitBlockExitNode(node: InitBlockExitNode, data: Unit): CFGNode<*> {
        return InitBlockExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitBlockEnterNode(node: BlockEnterNode, data: Unit): CFGNode<*> {
        return BlockEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitBlockExitNode(node: BlockExitNode, data: Unit): CFGNode<*> {
        return BlockExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenEnterNode(node: WhenEnterNode, data: Unit): CFGNode<*> {
        return WhenEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenExitNode(node: WhenExitNode, data: Unit): CFGNode<*> {
        return WhenExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenBranchConditionEnterNode(node: WhenBranchConditionEnterNode, data: Unit): CFGNode<*> {
        return WhenBranchConditionEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenBranchConditionExitNode(node: WhenBranchConditionExitNode, data: Unit): CFGNode<*> {
        return WhenBranchConditionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenBranchResultEnterNode(node: WhenBranchResultEnterNode, data: Unit): CFGNode<*> {
        return WhenBranchResultEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenBranchResultExitNode(node: WhenBranchResultExitNode, data: Unit): CFGNode<*> {
        return WhenBranchResultExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenSyntheticElseBranchNode(node: WhenSyntheticElseBranchNode, data: Unit): CFGNode<*> {
        return WhenSyntheticElseBranchNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLoopEnterNode(node: LoopEnterNode, data: Unit): CFGNode<*> {
        return LoopEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: Unit): CFGNode<*> {
        return LoopBlockEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: Unit): CFGNode<*> {
        return LoopBlockExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: Unit): CFGNode<*> {
        return LoopConditionEnterNode(node.owner, node.fir, node.loop, node.level).withData(node)
    }

    override fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: Unit): CFGNode<*> {
        return LoopConditionExitNode(node.owner, node.fir, node.loop, node.level).withData(node)
    }

    override fun visitLoopExitNode(node: LoopExitNode, data: Unit): CFGNode<*> {
        return LoopExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: Unit): CFGNode<*> {
        return TryExpressionEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: Unit): CFGNode<*> {
        return TryMainBlockEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: Unit): CFGNode<*> {
        return TryMainBlockExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: Unit): CFGNode<*> {
        return CatchClauseEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: Unit): CFGNode<*> {
        return CatchClauseExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: Unit): CFGNode<*> {
        return FinallyBlockEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: Unit): CFGNode<*> {
        return FinallyBlockExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: Unit): CFGNode<*> {
        return TryExpressionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: Unit): CFGNode<*> {
        return BooleanOperatorEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: Unit): CFGNode<*> {
        return BooleanOperatorExitLeftOperandNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: Unit): CFGNode<*> {
        return BooleanOperatorEnterRightOperandNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: Unit): CFGNode<*> {
        val leftOperandNode = get(node.leftOperandNode)
        val rightOperandNode = get(node.rightOperandNode)
        return BooleanOperatorExitNode(node.owner, node.fir, leftOperandNode, rightOperandNode, node.level).withData(node)
    }

    override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: Unit): CFGNode<*> {
        return TypeOperatorCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: Unit): CFGNode<*> {
        return ComparisonExpressionNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitEqualityOperatorCallNode(node: EqualityOperatorCallNode, data: Unit): CFGNode<*> {
        return EqualityOperatorCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitJumpNode(node: JumpNode, data: Unit): CFGNode<*> {
        return JumpNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: Unit): CFGNode<*> {
        return LiteralExpressionNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitCheckNotNullCallNode(node: CheckNotNullCallNode, data: Unit): CFGNode<*> {
        return CheckNotNullCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Unit): CFGNode<*> {
        return QualifiedAccessNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitResolvedQualifierNode(node: ResolvedQualifierNode, data: Unit): CFGNode<*> {
        return ResolvedQualifierNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: Unit): CFGNode<*> {
        return FunctionCallArgumentsEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: Unit): CFGNode<*> {
        return FunctionCallArgumentsExitNode(node.owner, node.fir, node.explicitReceiverExitNode, node.level).withData(node)
    }

    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: Unit): CFGNode<*> {
        return FunctionCallEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: Unit): CFGNode<*> {
        return FunctionCallExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitCallableReferenceNode(node: CallableReferenceNode, data: Unit): CFGNode<*> {
        return CallableReferenceNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitGetClassCallNode(node: GetClassCallNode, data: Unit): CFGNode<*> {
        return GetClassCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitDelegatedConstructorCallNode(node: DelegatedConstructorCallNode, data: Unit): CFGNode<*> {
        return DelegatedConstructorCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitStringConcatenationCallNode(node: StringConcatenationCallNode, data: Unit): CFGNode<*> {
        return StringConcatenationCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: Unit): CFGNode<*> {
        return ThrowExceptionNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitStubNode(node: StubNode, data: Unit): CFGNode<*> {
        return StubNode(node.owner, node.level).withData(node)
    }

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: Unit): CFGNode<*> {
        return VariableDeclarationNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Unit): CFGNode<*> {
        return VariableAssignmentNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitEnterSafeCallNode(node: EnterSafeCallNode, data: Unit): CFGNode<*> {
        return EnterSafeCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitExitSafeCallNode(node: ExitSafeCallNode, data: Unit): CFGNode<*> {
        return ExitSafeCallNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitWhenSubjectExpressionExitNode(node: WhenSubjectExpressionExitNode, data: Unit): CFGNode<*> {
        return WhenSubjectExpressionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitElvisLhsExitNode(node: ElvisLhsExitNode, data: Unit): CFGNode<*> {
        return ElvisLhsExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitElvisLhsIsNotNullNode(node: ElvisLhsIsNotNullNode, data: Unit): CFGNode<*> {
        return ElvisLhsIsNotNullNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitElvisRhsEnterNode(node: ElvisRhsEnterNode, data: Unit): CFGNode<*> {
        return ElvisRhsEnterNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitElvisExitNode(node: ElvisExitNode, data: Unit): CFGNode<*> {
        return ElvisExitNode(node.owner, node.fir, node.level).withData(node)
    }

    override fun visitSmartCastExpressionExitNode(node: SmartCastExpressionExitNode, data: Unit): CFGNode<*> {
        return SmartCastExpressionExitNode(node.owner, node.fir, node.level).withData(node)
    }

    @OptIn(CfgInternals::class)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun <E : FirElement, N : CFGNode<E>> N.withData(from: N): N {
        copyData(from, ::get)
        return this
    }
}