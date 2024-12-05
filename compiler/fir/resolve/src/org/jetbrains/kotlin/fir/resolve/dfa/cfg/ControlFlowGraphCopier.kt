/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement

internal class ControlFlowGraphCopier : ControlFlowGraphVisitor<CFGNode<*>, Unit>() {
    private val cachedGraphs = HashMap<ControlFlowGraph, ControlFlowGraph>()
    private val cachedNodes = HashMap<CFGNode<*>, CFGNode<*>>()

    operator fun get(graph: ControlFlowGraph): ControlFlowGraph {
        if (graph.isComplete) {
            // Do not copy the already complete graph – it's effectively immutable anyway
            return graph
        }

        val cachedGraph = cachedGraphs.get(graph)
        if (cachedGraph != null) {
            return cachedGraph
        }

        val newGraph = ControlFlowGraph(graph.declaration, graph.name, graph.kind)
        cachedGraphs.put(graph, newGraph)

        @OptIn(CfgInternals::class)
        newGraph.copyData(graph, ::get)

        return newGraph
    }

    operator fun <E : FirElement, N : CFGNode<E>> get(node: N): N {
        if (node.owner.isComplete) {
            return node
        }

        val cachedNode = cachedNodes.get(node)
        if (cachedNode != null) {
            @Suppress("UNCHECKED_CAST")
            return cachedNode as N
        }

        val newNode = node.accept(this, Unit)
        cachedNodes.put(node, newNode)

        @OptIn(CfgInternals::class)
        newNode.copyData(node, ::get)

        @Suppress("UNCHECKED_CAST")
        return newNode as N
    }

    override fun visitNode(node: CFGNode<*>, data: Unit): CFGNode<*> {
        error("Copying is not implemented for ${node::class.simpleName}")
    }

    override fun visitFunctionEnterNode(node: FunctionEnterNode, data: Unit): CFGNode<*> {
        return FunctionEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFunctionExitNode(node: FunctionExitNode, data: Unit): CFGNode<*> {
        return FunctionExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Unit): CFGNode<*> {
        return LocalFunctionDeclarationNode(get(node.owner), node.fir, node.level)
    }

    override fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: Unit): CFGNode<*> {
        return EnterValueParameterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: Unit): CFGNode<*> {
        return EnterDefaultArgumentsNode(get(node.owner), node.fir, node.level)
    }

    override fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: Unit): CFGNode<*> {
        return ExitDefaultArgumentsNode(get(node.owner), node.fir, node.level)
    }

    override fun visitExitValueParameterNode(node: ExitValueParameterNode, data: Unit): CFGNode<*> {
        return ExitValueParameterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: Unit): CFGNode<*> {
        return SplitPostponedLambdasNode(get(node.owner), node.fir, node.lambdas, node.level)
    }

    override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: Unit): CFGNode<*> {
        return PostponedLambdaExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: Unit): CFGNode<*> {
        return MergePostponedLambdaExitsNode(get(node.owner), node.fir, node.level)
    }

    override fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: Unit): CFGNode<*> {
        return AnonymousFunctionCaptureNode(get(node.owner), node.fir, node.level)
    }

    override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: Unit): CFGNode<*> {
        return AnonymousFunctionExpressionNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFileEnterNode(node: FileEnterNode, data: Unit): CFGNode<*> {
        return FileEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFileExitNode(node: FileExitNode, data: Unit): CFGNode<*> {
        return FileExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitAnonymousObjectEnterNode(node: AnonymousObjectEnterNode, data: Unit): CFGNode<*> {
        return AnonymousObjectEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitAnonymousObjectExpressionExitNode(node: AnonymousObjectExpressionExitNode, data: Unit): CFGNode<*> {
        return AnonymousObjectExpressionExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitClassEnterNode(node: ClassEnterNode, data: Unit): CFGNode<*> {
        return ClassEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitClassExitNode(node: ClassExitNode, data: Unit): CFGNode<*> {
        return ClassExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLocalClassExitNode(node: LocalClassExitNode, data: Unit): CFGNode<*> {
        return LocalClassExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitScriptEnterNode(node: ScriptEnterNode, data: Unit): CFGNode<*> {
        return ScriptEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitScriptExitNode(node: ScriptExitNode, data: Unit): CFGNode<*> {
        return ScriptExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: Unit): CFGNode<*> {
        return CodeFragmentEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: Unit): CFGNode<*> {
        return CodeFragmentExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitReplSnippetEnterNode(node: ReplSnippetEnterNode, data: Unit): CFGNode<*> {
        return ReplSnippetEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitReplSnippetExitNode(node: ReplSnippetExitNode, data: Unit): CFGNode<*> {
        return ReplSnippetExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitPropertyInitializerEnterNode(node: PropertyInitializerEnterNode, data: Unit): CFGNode<*> {
        return PropertyInitializerEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitPropertyInitializerExitNode(node: PropertyInitializerExitNode, data: Unit): CFGNode<*> {
        return PropertyInitializerExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitDelegateExpressionExitNode(node: DelegateExpressionExitNode, data: Unit): CFGNode<*> {
        return DelegateExpressionExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: Unit): CFGNode<*> {
        return FieldInitializerEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: Unit): CFGNode<*> {
        return FieldInitializerExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitInitBlockEnterNode(node: InitBlockEnterNode, data: Unit): CFGNode<*> {
        return InitBlockEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitInitBlockExitNode(node: InitBlockExitNode, data: Unit): CFGNode<*> {
        return InitBlockExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitBlockEnterNode(node: BlockEnterNode, data: Unit): CFGNode<*> {
        return BlockEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitBlockExitNode(node: BlockExitNode, data: Unit): CFGNode<*> {
        return BlockExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenEnterNode(node: WhenEnterNode, data: Unit): CFGNode<*> {
        return WhenEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenExitNode(node: WhenExitNode, data: Unit): CFGNode<*> {
        return WhenExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenBranchConditionEnterNode(node: WhenBranchConditionEnterNode, data: Unit): CFGNode<*> {
        return WhenBranchConditionEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenBranchConditionExitNode(node: WhenBranchConditionExitNode, data: Unit): CFGNode<*> {
        return WhenBranchConditionExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenBranchResultEnterNode(node: WhenBranchResultEnterNode, data: Unit): CFGNode<*> {
        return WhenBranchResultEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenBranchResultExitNode(node: WhenBranchResultExitNode, data: Unit): CFGNode<*> {
        return WhenBranchResultExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenSyntheticElseBranchNode(node: WhenSyntheticElseBranchNode, data: Unit): CFGNode<*> {
        return WhenSyntheticElseBranchNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLoopEnterNode(node: LoopEnterNode, data: Unit): CFGNode<*> {
        return LoopEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: Unit): CFGNode<*> {
        return LoopBlockEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: Unit): CFGNode<*> {
        return LoopBlockExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: Unit): CFGNode<*> {
        return LoopConditionEnterNode(get(node.owner), node.fir, node.loop, node.level)
    }

    override fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: Unit): CFGNode<*> {
        return LoopConditionExitNode(get(node.owner), node.fir, node.loop, node.level)
    }

    override fun visitLoopExitNode(node: LoopExitNode, data: Unit): CFGNode<*> {
        return LoopExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: Unit): CFGNode<*> {
        return TryExpressionEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: Unit): CFGNode<*> {
        return TryMainBlockEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: Unit): CFGNode<*> {
        return TryMainBlockExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: Unit): CFGNode<*> {
        return CatchClauseEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: Unit): CFGNode<*> {
        return CatchClauseExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: Unit): CFGNode<*> {
        return FinallyBlockEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: Unit): CFGNode<*> {
        return FinallyBlockExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: Unit): CFGNode<*> {
        return TryExpressionExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: Unit): CFGNode<*> {
        return BooleanOperatorEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: Unit): CFGNode<*> {
        return BooleanOperatorExitLeftOperandNode(get(node.owner), node.fir, node.level)
    }

    override fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: Unit): CFGNode<*> {
        return BooleanOperatorEnterRightOperandNode(get(node.owner), node.fir, node.level)
    }

    override fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: Unit): CFGNode<*> {
        val leftOperandNode = get(node.leftOperandNode)
        val rightOperandNode = get(node.rightOperandNode)
        return BooleanOperatorExitNode(get(node.owner), node.fir, leftOperandNode, rightOperandNode, node.level)
    }

    override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: Unit): CFGNode<*> {
        return TypeOperatorCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: Unit): CFGNode<*> {
        return ComparisonExpressionNode(get(node.owner), node.fir, node.level)
    }

    override fun visitEqualityOperatorCallNode(node: EqualityOperatorCallNode, data: Unit): CFGNode<*> {
        return EqualityOperatorCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitJumpNode(node: JumpNode, data: Unit): CFGNode<*> {
        return JumpNode(get(node.owner), node.fir, node.level)
    }

    override fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: Unit): CFGNode<*> {
        return LiteralExpressionNode(get(node.owner), node.fir, node.level)
    }

    override fun visitCheckNotNullCallNode(node: CheckNotNullCallNode, data: Unit): CFGNode<*> {
        return CheckNotNullCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Unit): CFGNode<*> {
        return QualifiedAccessNode(get(node.owner), node.fir, node.level)
    }

    override fun visitResolvedQualifierNode(node: ResolvedQualifierNode, data: Unit): CFGNode<*> {
        return ResolvedQualifierNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: Unit): CFGNode<*> {
        return FunctionCallArgumentsEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: Unit): CFGNode<*> {
        return FunctionCallArgumentsExitNode(get(node.owner), node.fir, node.explicitReceiverExitNode, node.level)
    }

    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: Unit): CFGNode<*> {
        return FunctionCallEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: Unit): CFGNode<*> {
        return FunctionCallExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitCallableReferenceNode(node: CallableReferenceNode, data: Unit): CFGNode<*> {
        return CallableReferenceNode(get(node.owner), node.fir, node.level)
    }

    override fun visitGetClassCallNode(node: GetClassCallNode, data: Unit): CFGNode<*> {
        return GetClassCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitDelegatedConstructorCallNode(node: DelegatedConstructorCallNode, data: Unit): CFGNode<*> {
        return DelegatedConstructorCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitStringConcatenationCallNode(node: StringConcatenationCallNode, data: Unit): CFGNode<*> {
        return StringConcatenationCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: Unit): CFGNode<*> {
        return ThrowExceptionNode(get(node.owner), node.fir, node.level)
    }

    override fun visitStubNode(node: StubNode, data: Unit): CFGNode<*> {
        return StubNode(get(node.owner), node.level)
    }

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: Unit): CFGNode<*> {
        return VariableDeclarationNode(get(node.owner), node.fir, node.level)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Unit): CFGNode<*> {
        return VariableAssignmentNode(get(node.owner), node.fir, node.level)
    }

    override fun visitEnterSafeCallNode(node: EnterSafeCallNode, data: Unit): CFGNode<*> {
        return EnterSafeCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitExitSafeCallNode(node: ExitSafeCallNode, data: Unit): CFGNode<*> {
        return ExitSafeCallNode(get(node.owner), node.fir, node.level)
    }

    override fun visitWhenSubjectExpressionExitNode(node: WhenSubjectExpressionExitNode, data: Unit): CFGNode<*> {
        return WhenSubjectExpressionExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitElvisLhsExitNode(node: ElvisLhsExitNode, data: Unit): CFGNode<*> {
        return ElvisLhsExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitElvisLhsIsNotNullNode(node: ElvisLhsIsNotNullNode, data: Unit): CFGNode<*> {
        return ElvisLhsIsNotNullNode(get(node.owner), node.fir, node.level)
    }

    override fun visitElvisRhsEnterNode(node: ElvisRhsEnterNode, data: Unit): CFGNode<*> {
        return ElvisRhsEnterNode(get(node.owner), node.fir, node.level)
    }

    override fun visitElvisExitNode(node: ElvisExitNode, data: Unit): CFGNode<*> {
        return ElvisExitNode(get(node.owner), node.fir, node.level)
    }

    override fun visitSmartCastExpressionExitNode(node: SmartCastExpressionExitNode, data: Unit): CFGNode<*> {
        return SmartCastExpressionExitNode(get(node.owner), node.fir, node.level)
    }
}