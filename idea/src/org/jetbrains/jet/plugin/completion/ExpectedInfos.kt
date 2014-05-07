/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.completion

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetValueArgument
import org.jetbrains.jet.lang.psi.JetValueArgumentList
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import com.intellij.lang.ASTNode
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo
import org.jetbrains.jet.lang.resolve.calls.context.ContextDependency
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode
import org.jetbrains.jet.lang.resolve.calls.CompositeExtension
import org.jetbrains.jet.di.InjectorForMacros
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetContainerNode
import org.jetbrains.jet.plugin.completion.smart.isSubtypeOf
import org.jetbrains.jet.lang.resolve.calls.util.DelegatingCall
import org.jetbrains.jet.lang.resolve.calls.util.noErrorsInValueArguments
import org.jetbrains.jet.lang.resolve.calls.util.hasUnmappedParameters
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.plugin.util.makeNullable
import org.jetbrains.jet.plugin.util.makeNotNullable

enum class Tail {
    COMMA
    RPARENTH
    ELSE
}

data class ExpectedInfo(val `type`: JetType, val tail: Tail?)

class ExpectedInfos(val bindingContext: BindingContext, val moduleDescriptor: ModuleDescriptor) {
    public fun calculate(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        return calculateForArgument(expressionWithType)
            ?: calculateForFunctionLiteralArgument(expressionWithType)
            ?: calculateForEq(expressionWithType)
            ?: calculateForIf(expressionWithType)
            ?: calculateForElvis(expressionWithType)
            ?: calculateForBlockExpression(expressionWithType)
            ?: getFromBindingContext(expressionWithType)
    }

    private fun calculateForArgument(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val argument = expressionWithType.getParent() as? JetValueArgument ?: return null
        if (argument.isNamed()) return null //TODO - support named arguments (also do not forget to check for presence of named arguments before)
        val argumentList = argument.getParent() as JetValueArgumentList
        val argumentIndex = argumentList.getArguments().indexOf(argument)
        val callExpression = argumentList.getParent() as? JetCallExpression ?: return null
        return calculateForArgument(callExpression, argumentIndex, false)
    }

    private fun calculateForFunctionLiteralArgument(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val callExpression = expressionWithType.getParent() as? JetCallExpression
        if (callExpression != null) {
            val arguments = callExpression.getFunctionLiteralArguments()
            if (arguments.firstOrNull() == expressionWithType) {
                return calculateForArgument(callExpression, callExpression.getValueArguments().size, true)
            }
        }
        return null
    }

    private fun calculateForArgument(callExpression: JetCallExpression, argumentIndex: Int, isFunctionLiteralArgument: Boolean): Collection<ExpectedInfo>? {
        val calleeExpression = callExpression.getCalleeExpression()

        val parent = callExpression.getParent()
        val receiver: ReceiverValue
        val callOperationNode: ASTNode?
        if (parent is JetQualifiedExpression && callExpression == parent.getSelectorExpression()) {
            val receiverExpression = parent.getReceiverExpression()
            val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, receiverExpression] ?: return null
            receiver = ExpressionReceiver(receiverExpression, expressionType)
            callOperationNode = parent.getOperationTokenNode()
        }
        else {
            receiver = ReceiverValue.NO_RECEIVER
            callOperationNode = null
        }
        var call = CallMaker.makeCall(receiver, callOperationNode, callExpression)

        if (!isFunctionLiteralArgument) { // leave only arguments before the current one
            call = object : DelegatingCall(call) {
                override fun getValueArguments() = super.getValueArguments().subList(0, argumentIndex)
                override fun getValueArgumentList() = null
            }
        }

        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, calleeExpression] ?: return null //TODO: discuss it

        val callResolutionContext = BasicCallResolutionContext.create(
                DelegatingBindingTrace(bindingContext, "Temporary trace for completion"),
                resolutionScope,
                call,
                bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, callExpression] ?: TypeUtils.NO_EXPECTED_TYPE,
                bindingContext[BindingContext.EXPRESSION_DATA_FLOW_INFO, callExpression] ?: DataFlowInfo.EMPTY,
                ContextDependency.INDEPENDENT,
                CheckValueArgumentsMode.ENABLED,
                CompositeExtension(listOf()),
                false).replaceCollectAllCandidates(true)
        val callResolver = InjectorForMacros(callExpression.getProject(), moduleDescriptor).getCallResolver()!!
        val results: OverloadResolutionResults<FunctionDescriptor> = callResolver.resolveFunctionCall(callResolutionContext)

        val expectedInfos = HashSet<ExpectedInfo>()
        for (candidate: ResolvedCall<FunctionDescriptor> in results.getAllCandidates()!!) {
            // consider only candidates with more arguments than in the truncated call and with all arguments before the current one matched
            if (candidate.noErrorsInValueArguments() && (isFunctionLiteralArgument || candidate.hasUnmappedParameters())) {
                val descriptor = candidate.getResultingDescriptor()
                if (!Visibilities.isVisible(descriptor, resolutionScope.getContainingDeclaration())) continue

                val parameters = descriptor.getValueParameters()
                if (isFunctionLiteralArgument) {
                    if (argumentIndex != parameters.size - 1) continue
                }
                else {
                    if (parameters.size <= argumentIndex) continue
                }
                val parameterDescriptor = parameters[argumentIndex]
                val tail = if (isFunctionLiteralArgument) null else if (argumentIndex == parameters.size - 1) Tail.RPARENTH else Tail.COMMA
                expectedInfos.add(ExpectedInfo(parameterDescriptor.getType(), tail))
            }
        }
        return expectedInfos
    }

    private fun calculateForEq(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.getParent() as? JetBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.getOperationToken()
            if (operationToken == JetTokens.EQEQ || operationToken == JetTokens.EXCLEQ) {
                val otherOperand = if (expressionWithType == binaryExpression.getRight()) binaryExpression.getLeft() else binaryExpression.getRight()
                if (otherOperand != null) {
                    val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, otherOperand] ?: return null
                    return listOf(ExpectedInfo(expressionType.makeNullable(), null))
                }
            }
        }
        return null
    }

    private fun calculateForIf(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val ifExpression = (expressionWithType.getParent() as? JetContainerNode)?.getParent() as? JetIfExpression ?: return null
        return when (expressionWithType) {
            ifExpression.getCondition() -> listOf(ExpectedInfo(KotlinBuiltIns.getInstance().getBooleanType(), Tail.RPARENTH))

            ifExpression.getThen() -> calculate(ifExpression)?.map { ExpectedInfo(it.`type`, Tail.ELSE) }

            ifExpression.getElse() -> {
                val ifExpectedInfo = calculate(ifExpression)
                val thenType = bindingContext[BindingContext.EXPRESSION_TYPE, ifExpression.getThen()]
                if (thenType != null)
                    ifExpectedInfo?.filter { it.`type`.isSubtypeOf(thenType) }
                else
                    ifExpectedInfo
            }

            else -> return null
        }
    }

    private fun calculateForElvis(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.getParent() as? JetBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.getOperationToken()
            if (operationToken == JetTokens.ELVIS && expressionWithType == binaryExpression.getRight()) {
                val leftExpression = binaryExpression.getLeft() ?: return null
                val leftType = bindingContext[BindingContext.EXPRESSION_TYPE, leftExpression]
                val leftTypeNotNullable = leftType?.makeNotNullable()
                val expectedInfos = calculate(binaryExpression)
                if (expectedInfos != null) {
                    return if (leftTypeNotNullable != null)
                        expectedInfos.filter { leftTypeNotNullable.isSubtypeOf(it.`type`) }
                    else
                        expectedInfos
                }
                else if (leftTypeNotNullable != null) {
                    return listOf(ExpectedInfo(leftTypeNotNullable, null))
                }
            }
        }
        return null
    }

    private fun calculateForBlockExpression(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val block = expressionWithType.getParent() as? JetBlockExpression ?: return null
        if (expressionWithType != block.getStatements().last()) return null
        return calculate(block)?.map { ExpectedInfo(it.`type`, null) }
    }

    private fun getFromBindingContext(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
        return listOf(ExpectedInfo(expectedType, null))
    }
}