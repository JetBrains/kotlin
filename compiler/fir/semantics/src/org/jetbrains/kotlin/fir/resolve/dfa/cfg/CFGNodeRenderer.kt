/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirCallNoArgumentsRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer

fun CFGNode<*>.render(): String =
    buildString {
        append(
            when (this@render) {
                is FunctionEnterNode -> "Enter function ${owner.name}"
                is FunctionExitNode -> "Exit function ${owner.name}"
                is LocalFunctionDeclarationNode -> "Local function declaration"

                is BlockEnterNode -> "Enter block"
                is BlockExitNode -> "Exit block"

                is WhenEnterNode -> "Enter when"
                is WhenBranchConditionEnterNode -> "Enter when branch condition ${if (fir.condition is FirElseIfTrueCondition) "\"else\"" else ""}"
                is WhenBranchConditionExitNode -> "Exit when branch condition"
                is WhenBranchResultEnterNode -> "Enter when branch result"
                is WhenBranchResultExitNode -> "Exit when branch result"
                is WhenSyntheticElseBranchNode -> "Synthetic else branch"
                is WhenExitNode -> "Exit when"

                is LoopEnterNode -> "Enter ${fir.type()} loop"
                is LoopBlockEnterNode -> "Enter loop block"
                is LoopBlockExitNode -> "Exit loop block"
                is LoopConditionEnterNode -> "Enter loop condition"
                is LoopConditionExitNode -> "Exit loop condition"
                is LoopExitNode -> "Exit ${fir.type()} loop"

                is QualifiedAccessNode -> "Access variable ${CfgRenderer.renderElementAsString(fir.calleeReference)}"
                is ResolvedQualifierNode -> "Access qualifier ${fir.classId}"
                is ComparisonExpressionNode -> "Comparison ${fir.operation.operator}"
                is TypeOperatorCallNode -> "Type operator: \"${CfgRenderer.renderElementAsString(fir)}\""
                is SmartCastExpressionExitNode -> "Smart cast: \"${CfgRenderer.renderElementAsString(fir)}\""
                is EqualityOperatorCallNode -> "Equality operator ${fir.operation.operator}"
                is JumpNode -> "Jump: ${fir.render()}"
                is StubNode -> "Stub"
                is CheckNotNullCallNode -> "Check not null: ${CfgRenderer.renderElementAsString(fir)}"

                is LiteralExpressionNode -> "Const: ${fir.render()}"
                is VariableDeclarationNode ->
                    "Variable declaration: ${
                        CfgRenderer.renderAsCallableDeclarationString(fir)
                    }"

                is VariableAssignmentNode -> "Assignment: ${fir.calleeReference?.let(CfgRenderer::renderElementAsString)}"
                is FunctionCallArgumentsEnterNode -> "Function call arguments enter"
                is FunctionCallArgumentsExitNode -> "Function call arguments exit"
                is FunctionCallEnterNode -> "Function call enter: ${CfgRenderer.renderElementAsString(fir)}"
                is FunctionCallExitNode -> "Function call exit: ${CfgRenderer.renderElementAsString(fir)}"
                is DelegatedConstructorCallNode -> "Delegated constructor call: ${CfgRenderer.renderElementAsString(fir)}"
                is StringConcatenationCallNode -> "String concatenation call: ${CfgRenderer.renderElementAsString(fir)}"
                is ThrowExceptionNode -> "Throw: ${CfgRenderer.renderElementAsString(fir)}"

                is TryExpressionEnterNode -> "Try expression enter"
                is TryMainBlockEnterNode -> "Try main block enter"
                is TryMainBlockExitNode -> "Try main block exit"
                is CatchClauseEnterNode -> "Catch enter"
                is CatchClauseExitNode -> "Catch exit"
                is FinallyBlockEnterNode -> "Enter finally"
                is FinallyBlockExitNode -> "Exit finally"
                is TryExpressionExitNode -> "Try expression exit"

                is BooleanOperatorEnterNode -> "Enter " + if (fir.kind == LogicOperationKind.AND) "&&" else "||"
                is BooleanOperatorExitLeftOperandNode -> "Exit left part of " + if (fir.kind == LogicOperationKind.AND) "&&" else "||"
                is BooleanOperatorEnterRightOperandNode -> "Enter right part of " + if (fir.kind == LogicOperationKind.AND) "&&" else "||"
                is BooleanOperatorExitNode -> "Exit " + if (fir.kind == LogicOperationKind.AND) "&&" else "||"

                is PropertyInitializerEnterNode -> "Enter property"
                is PropertyInitializerExitNode -> "Exit property"
                is DelegateExpressionExitNode -> "Exit property delegate"
                is FieldInitializerEnterNode -> "Enter field"
                is FieldInitializerExitNode -> "Exit field"
                is InitBlockEnterNode -> "Enter init block"
                is InitBlockExitNode -> "Exit init block"

                is EnterSafeCallNode -> "Enter safe call"
                is ExitSafeCallNode -> "Exit safe call"

                is WhenSubjectExpressionExitNode -> "Exit ${'$'}subj"

                is SplitPostponedLambdasNode -> "Postponed enter to lambda"
                is PostponedLambdaExitNode -> "Postponed exit from lambda"
                is MergePostponedLambdaExitsNode -> "Merge postponed lambda exits"
                is AnonymousFunctionExpressionNode -> "Exit anonymous function expression"
                is AnonymousFunctionCaptureNode -> "Anonymous function capture"

                is FileEnterNode -> "Enter file ${fir.name}"
                is FileExitNode -> "Exit file ${fir.name}"

                is ClassEnterNode -> "Enter class ${owner.name}"
                is ClassExitNode -> "Exit class ${owner.name}"
                is LocalClassExitNode -> "Local class declaration"
                is AnonymousObjectEnterNode -> "Enter anonymous object"
                is AnonymousObjectExpressionExitNode -> "Exit anonymous object expression"

                is ScriptEnterNode -> "Enter class ${fir.name}"
                is ScriptExitNode -> "Exit class ${fir.name}"

                is CodeFragmentEnterNode -> "Enter code fragment"
                is CodeFragmentExitNode -> "Exit code fragment"

                is ReplSnippetEnterNode -> "Enter repl snippet"
                is ReplSnippetExitNode -> "Exit repl snippet"

                is FakeExpressionEnterNode -> "Enter fake expression"

                is EnterValueParameterNode -> "Enter default value of ${fir.name}"
                is EnterDefaultArgumentsNode -> "Enter default value of ${fir.name}"
                is ExitDefaultArgumentsNode -> "Exit default value of ${fir.name}"
                is ExitValueParameterNode -> "Exit default value of ${fir.name}"

                is ElvisLhsExitNode -> "Exit lhs of ?:"
                is ElvisLhsIsNotNullNode -> "Lhs of ?: is not null"
                is ElvisRhsEnterNode -> "Enter rhs of ?:"
                is ElvisExitNode -> "Exit ?:"

                is CallableReferenceNode -> "Callable reference: ${CfgRenderer.renderElementAsString(fir)}"
                is GetClassCallNode -> "::class call"
            },
        )
    }

// NB: renderer has a state, so we have to create it each time
private val CfgRenderer
    get() = FirRenderer(annotationRenderer = null, callArgumentsRenderer = FirCallNoArgumentsRenderer())

private fun FirLoop.type(): String = when (this) {
    is FirWhileLoop -> "while"
    is FirDoWhileLoop -> "do-while"
    else -> throw IllegalArgumentException()
}
