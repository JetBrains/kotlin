/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.JetValueArgumentList
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.context.CheckValueArgumentsMode
import org.jetbrains.kotlin.resolve.calls.checkers.CompositeChecker
import org.jetbrains.kotlin.di.InjectorForMacros
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import java.util.HashSet
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetContainerNode
import org.jetbrains.kotlin.resolve.calls.callUtil.noErrorsInValueArguments
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.psi.JetWhenConditionWithExpression
import org.jetbrains.kotlin.psi.JetWhenEntry
import org.jetbrains.kotlin.psi.JetWhenExpression
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.psi.JetPrefixExpression
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.psi.JetFunctionLiteralArgument
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetReturnExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.idea.completion.smart.toList
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

enum class Tail {
    COMMA
    RPARENTH
    ELSE
}

open data class ExpectedInfo(val type: JetType, val name: String?, val tail: Tail?)

class PositionalArgumentExpectedInfo(type: JetType, name: String?, tail: Tail?, val function: FunctionDescriptor, val argumentIndex: Int)
  : ExpectedInfo(type, name, tail) {

    override fun equals(other: Any?)
            = other is PositionalArgumentExpectedInfo && super.equals(other) && function == other.function && argumentIndex == other.argumentIndex

    override fun hashCode()
            = function.hashCode()
}

class ExpectedInfos(val bindingContext: BindingContext, val resolutionFacade: ResolutionFacade) {
    public fun calculate(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        return calculateForArgument(expressionWithType)
            ?: calculateForFunctionLiteralArgument(expressionWithType)
            ?: calculateForEqAndAssignment(expressionWithType)
            ?: calculateForIf(expressionWithType)
            ?: calculateForElvis(expressionWithType)
            ?: calculateForBlockExpression(expressionWithType)
            ?: calculateForWhenEntryValue(expressionWithType)
            ?: calculateForExclOperand(expressionWithType)
            ?: calculateForInitializer(expressionWithType)
            ?: calculateForExpressionBody(expressionWithType)
            ?: calculateForReturn(expressionWithType)
            ?: getFromBindingContext(expressionWithType)
    }

    private fun calculateForArgument(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val argument = expressionWithType.getParent() as? JetValueArgument ?: return null
        if (argument.isNamed()) return null //TODO - support named arguments (also do not forget to check for presence of named arguments before)
        val argumentList = argument.getParent() as? JetValueArgumentList ?: return null
        val argumentIndex = argumentList.getArguments().indexOf(argument)
        val callElement = argumentList.getParent() as? JetCallElement ?: return null
        return calculateForArgument(callElement, argumentIndex, false)
    }

    private fun calculateForFunctionLiteralArgument(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val functionLiteralArgument = expressionWithType.getParent() as? JetFunctionLiteralArgument
        val callExpression = functionLiteralArgument?.getParent() as? JetCallExpression
        if (callExpression != null) {
            if (callExpression.getFunctionLiteralArguments().firstOrNull()?.getArgumentExpression() == expressionWithType) {
                return calculateForArgument(callExpression, callExpression.getValueArguments().size() - 1, true)
            }
        }
        return null
    }

    private fun calculateForArgument(callElement: JetCallElement, argumentIndex: Int, isFunctionLiteralArgument: Boolean): Collection<ExpectedInfo>? {
        val calleeExpression = callElement.getCalleeExpression()

        val parent = callElement.getParent()
        val receiver: ReceiverValue
        val callOperationNode: ASTNode?
        if (parent is JetQualifiedExpression && callElement == parent.getSelectorExpression()) {
            val receiverExpression = parent.getReceiverExpression()
            val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, receiverExpression]
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression]
            if (expressionType != null) {
                receiver = ExpressionReceiver(receiverExpression, expressionType)
                callOperationNode = parent.getOperationTokenNode()
            }
            else if (qualifier != null) {
                receiver = qualifier
                callOperationNode = null
            }
            else {
                return null
            }
        }
        else {
            receiver = ReceiverValue.NO_RECEIVER
            callOperationNode = null
        }
        var call = CallMaker.makeCall(receiver, callOperationNode, callElement)

        if (!isFunctionLiteralArgument) { // leave only arguments before the current one
            call = object : DelegatingCall(call) {
                override fun getValueArguments() = super.getValueArguments().subList(0, argumentIndex)
                override fun getValueArgumentList() = null
            }
        }

        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, calleeExpression] ?: return null //TODO: discuss it

        val expectedType = (callElement as? JetExpression)?.let { bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, it] } ?: TypeUtils.NO_EXPECTED_TYPE
        val dataFlowInfo = bindingContext.getDataFlowInfo(calleeExpression)
        val callResolutionContext = BasicCallResolutionContext.create(
                DelegatingBindingTrace(bindingContext, "Temporary trace for completion"),
                resolutionScope,
                call,
                expectedType,
                dataFlowInfo,
                ContextDependency.INDEPENDENT,
                CheckValueArgumentsMode.ENABLED,
                CompositeChecker(listOf()),
                false).replaceCollectAllCandidates(true)
        val callResolver = InjectorForMacros(
                callElement.getProject(),
                resolutionFacade.findModuleDescriptor(callElement)
        ).getCallResolver()
        val results: OverloadResolutionResults<FunctionDescriptor> = callResolver.resolveFunctionCall(callResolutionContext)

        val expectedInfos = HashSet<ExpectedInfo>()
        for (candidate: ResolvedCall<FunctionDescriptor> in results.getAllCandidates()!!) {
            val status = candidate.getStatus()
            if (status == ResolutionStatus.RECEIVER_TYPE_ERROR || status == ResolutionStatus.RECEIVER_PRESENCE_ERROR) continue

            // consider only candidates with more arguments than in the truncated call and with all arguments before the current one matched
            if (candidate.noErrorsInValueArguments() && (candidate.getCandidateDescriptor().getValueParameters().size() > argumentIndex || isFunctionLiteralArgument)) {
                val descriptor = candidate.getResultingDescriptor()

                val thisReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidate.getDispatchReceiver(), bindingContext)
                if (!Visibilities.isVisible(thisReceiver, descriptor, resolutionScope.getContainingDeclaration())) continue

                val parameters = descriptor.getValueParameters()
                if (isFunctionLiteralArgument && argumentIndex != parameters.lastIndex) continue

                val tail = if (isFunctionLiteralArgument)
                    null
                else if (argumentIndex == parameters.lastIndex)
                    Tail.RPARENTH //TODO: support square brackets
                else if (parameters.drop(argumentIndex + 1).all { it.hasDefaultValue() || it.getVarargElementType() != null })
                    null
                else
                    Tail.COMMA

                val parameter = parameters[argumentIndex]
                val expectedName = if (descriptor.hasSynthesizedParameterNames()) null else parameter.getName().asString()
                expectedInfos.add(PositionalArgumentExpectedInfo(parameter.getType(), expectedName, tail, descriptor, argumentIndex))
            }
        }
        return expectedInfos
    }

    private fun calculateForEqAndAssignment(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.getParent() as? JetBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.getOperationToken()
            if (operationToken == JetTokens.EQ || operationToken == JetTokens.EQEQ || operationToken == JetTokens.EXCLEQ
                || operationToken == JetTokens.EQEQEQ || operationToken == JetTokens.EXCLEQEQEQ) {
                val otherOperand = if (expressionWithType == binaryExpression.getRight()) binaryExpression.getLeft() else binaryExpression.getRight()
                if (otherOperand != null) {
                    val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, otherOperand] ?: return null
                    return listOf(ExpectedInfo(expressionType, expectedNameFromExpression(otherOperand), null))
                }
            }
        }
        return null
    }

    private fun calculateForIf(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val ifExpression = (expressionWithType.getParent() as? JetContainerNode)?.getParent() as? JetIfExpression ?: return null
        return when (expressionWithType) {
            ifExpression.getCondition() -> listOf(ExpectedInfo(KotlinBuiltIns.getInstance().getBooleanType(), null, Tail.RPARENTH))

            ifExpression.getThen() -> calculate(ifExpression)?.map { ExpectedInfo(it.type, it.name, Tail.ELSE) }

            ifExpression.getElse() -> {
                val ifExpectedInfo = calculate(ifExpression)
                val thenType = bindingContext[BindingContext.EXPRESSION_TYPE, ifExpression.getThen()]
                if (thenType != null)
                    ifExpectedInfo?.filter { it.type.isSubtypeOf(thenType) }
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
                        expectedInfos.filter { leftTypeNotNullable.isSubtypeOf(it.type) }
                    else
                        expectedInfos
                }
                else if (leftTypeNotNullable != null) {
                    return listOf(ExpectedInfo(leftTypeNotNullable, null, null))
                }
            }
        }
        return null
    }

    private fun calculateForBlockExpression(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val block = expressionWithType.getParent() as? JetBlockExpression ?: return null
        if (expressionWithType != block.getStatements().last()) return null
        return calculate(block)?.map { ExpectedInfo(it.type, it.name, null) }
    }

    private fun calculateForWhenEntryValue(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val condition = expressionWithType.getParent() as? JetWhenConditionWithExpression ?: return null
        val entry = condition.getParent() as JetWhenEntry
        val whenExpression = entry.getParent() as JetWhenExpression
        val subject = whenExpression.getSubjectExpression()
        if (subject != null) {
            val subjectType = bindingContext[BindingContext.EXPRESSION_TYPE, subject] ?: return null
            return listOf(ExpectedInfo(subjectType, null, null))
        }
        else {
            return listOf(ExpectedInfo(KotlinBuiltIns.getInstance().getBooleanType(), null, null))
        }
    }

    private fun calculateForExclOperand(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val prefixExpression = expressionWithType.getParent() as? JetPrefixExpression ?: return null
        if (prefixExpression.getOperationToken() != JetTokens.EXCL) return null
        return listOf(ExpectedInfo(KotlinBuiltIns.getInstance().getBooleanType(), null, null))
    }

    private fun calculateForInitializer(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val property = expressionWithType.getParent() as? JetProperty ?: return null
        if (expressionWithType != property.getInitializer()) return null
        val propertyDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? VariableDescriptor ?: return null
        return listOf(ExpectedInfo(propertyDescriptor.getType(), propertyDescriptor.getName().asString(), null))
    }

    private fun calculateForExpressionBody(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val declaration = expressionWithType.getParent() as? JetDeclarationWithBody ?: return null
        if (expressionWithType != declaration.getBodyExpression() || declaration.hasBlockBody()) return null
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? FunctionDescriptor ?: return null
        return functionReturnValueExpectedInfo(descriptor).toList()
    }

    private fun calculateForReturn(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val returnExpression = expressionWithType.getParent() as? JetReturnExpression ?: return null
        val descriptor = returnExpression.getTargetFunctionDescriptor(bindingContext) ?: return null
        return functionReturnValueExpectedInfo(descriptor).toList()
    }

    private fun functionReturnValueExpectedInfo(descriptor: FunctionDescriptor): ExpectedInfo? {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> ExpectedInfo(descriptor.getReturnType() ?: return null, descriptor.getName().asString(), null)

            is PropertyGetterDescriptor -> {
                if (descriptor !is PropertyGetterDescriptor) return null
                val property = descriptor.getCorrespondingProperty()
                ExpectedInfo(property.getType(),  property.getName().asString(), null)
            }

            else -> null
        }
    }

    private fun getFromBindingContext(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
        return listOf(ExpectedInfo(expectedType, null, null))
    }

    private fun expectedNameFromExpression(expression: JetExpression?): String? {
        return when (expression) {
            is JetSimpleNameExpression -> expression.getReferencedName()
            is JetQualifiedExpression -> expectedNameFromExpression(expression.getSelectorExpression())
            is JetCallExpression -> expectedNameFromExpression(expression.getCalleeExpression())
            is JetArrayAccessExpression -> expectedNameFromExpression(expression.getArrayExpression())?.fromPlural()
            else -> null
        }
    }

    private fun String.fromPlural()
            = if (endsWith("s")) substring(0, length() - 1) else this
}
