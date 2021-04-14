/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.render

fun CFGNode<*>.render(): String =
    buildString {
        append(
            when (this@render) {
                is FunctionEnterNode -> "Enter function \"${fir.name()}\""
                is FunctionExitNode -> "Exit function \"${fir.name()}\""
                is LocalFunctionDeclarationNode -> "Local function declaration ${owner.name}"

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
                is LoopExitNode -> "Exit ${fir.type()}loop"

                is QualifiedAccessNode -> "Access variable ${fir.calleeReference.render(CfgRenderMode)}"
                is ResolvedQualifierNode -> "Access qualifier ${fir.classId}"
                is ComparisonExpressionNode -> "Comparison ${fir.operation.operator}"
                is TypeOperatorCallNode -> "Type operator: \"${fir.render(CfgRenderMode)}\""
                is EqualityOperatorCallNode -> "Equality operator ${fir.operation.operator}"
                is JumpNode -> "Jump: ${fir.render()}"
                is StubNode -> "Stub"
                is CheckNotNullCallNode -> "Check not null: ${fir.render(CfgRenderMode)}"

                is ConstExpressionNode -> "Const: ${fir.render()}"
                is VariableDeclarationNode ->
                    "Variable declaration: ${buildString {
                        FirRenderer(
                            this,
                            CfgRenderMode
                        ).visitCallableDeclaration(fir)
                    }}"

                is VariableAssignmentNode -> "Assignment: ${fir.lValue.render(CfgRenderMode)}"
                is FunctionCallNode -> "Function call: ${fir.render(CfgRenderMode)}"
                is DelegatedConstructorCallNode -> "Delegated constructor call: ${fir.render(CfgRenderMode)}"
                is ThrowExceptionNode -> "Throw: ${fir.render(CfgRenderMode)}"

                is TryExpressionEnterNode -> "Try expression enter"
                is TryMainBlockEnterNode -> "Try main block enter"
                is TryMainBlockExitNode -> "Try main block exit"
                is CatchClauseEnterNode -> "Catch enter"
                is CatchClauseExitNode -> "Catch exit"
                is FinallyBlockEnterNode -> "Enter finally"
                is FinallyBlockExitNode -> "Exit finally"
                is FinallyProxyEnterNode -> TODO()
                is FinallyProxyExitNode -> TODO()
                is TryExpressionExitNode -> "Try expression exit"

                is BinaryAndEnterNode -> "Enter &&"
                is BinaryAndExitLeftOperandNode -> "Exit left part of &&"
                is BinaryAndEnterRightOperandNode -> "Enter right part of &&"
                is BinaryAndExitNode -> "Exit &&"
                is BinaryOrEnterNode -> "Enter ||"
                is BinaryOrExitLeftOperandNode -> "Exit left part of ||"
                is BinaryOrEnterRightOperandNode -> "Enter right part of ||"
                is BinaryOrExitNode -> "Exit ||"

                is PartOfClassInitializationNode -> "Part of class initialization"
                is PropertyInitializerEnterNode -> "Enter property"
                is PropertyInitializerExitNode -> "Exit property"
                is FieldInitializerEnterNode -> "Enter field"
                is FieldInitializerExitNode -> "Exit field"
                is InitBlockEnterNode -> "Enter init block"
                is InitBlockExitNode -> "Exit init block"
                is AnnotationEnterNode -> "Enter annotation"
                is AnnotationExitNode -> "Exit annotation"

                is EnterContractNode -> "Enter contract"
                is ExitContractNode -> "Exit contract"

                is EnterSafeCallNode -> "Enter safe call"
                is ExitSafeCallNode -> "Exit safe call"

                is PostponedLambdaEnterNode -> "Postponed enter to lambda"
                is PostponedLambdaExitNode -> "Postponed exit from lambda"

                is UnionFunctionCallArgumentsNode -> "Call arguments union"

                is ClassEnterNode -> "Enter class ${owner.name}"
                is ClassExitNode -> "Exit class ${owner.name}"
                is LocalClassExitNode -> "Exit local class ${owner.name}"
                is AnonymousObjectExitNode -> "Exit anonymous object"

                is ContractDescriptionEnterNode -> "Enter contract description"

                is EnterDefaultArgumentsNode -> "Enter default value of ${fir.name}"
                is ExitDefaultArgumentsNode -> "Exit default value of ${fir.name}"

                is ElvisLhsExitNode -> "Exit lhs of ?:"
                is ElvisLhsIsNotNullNode -> "Lhs of ?: is not null"
                is ElvisRhsEnterNode -> "Enter rhs of ?:"
                is ElvisExitNode -> "Exit ?:"

                is CallableReferenceNode -> "Callable reference: ${fir.render(CfgRenderMode)}"

                is AbstractBinaryExitNode -> throw IllegalStateException()
            },
        )
    }

private val CfgRenderMode = FirRenderer.RenderMode(
    renderLambdaBodies = false,
    renderCallArguments = false,
    renderCallableFqNames = false,
    renderDeclarationResolvePhase = false,
    renderAnnotation = false,
)

private fun FirFunction<*>.name(): String = when (this) {
    is FirSimpleFunction -> name.asString()
    is FirAnonymousFunction -> "anonymousFunction"
    is FirConstructor -> "<init>"
    is FirPropertyAccessor -> if (isGetter) "getter" else "setter"
    is FirErrorFunction -> "errorFunction"
    else -> TODO(toString())
}

private fun FirLoop.type(): String = when (this) {
    is FirWhileLoop -> "while"
    is FirDoWhileLoop -> "do-while"
    else -> throw IllegalArgumentException()
}
