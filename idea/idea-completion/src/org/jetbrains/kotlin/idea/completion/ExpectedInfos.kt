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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.di.InjectorForMacros
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.completion.smart.toList
import org.jetbrains.kotlin.idea.core.mapArgumentsToParameters
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.noErrorsInValueArguments
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CompositeChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckValueArgumentsMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.HashSet

enum class Tail {
    COMMA
    RPARENTH
    ELSE
}

data class ItemOptions(val starPrefix: Boolean) {
    companion object {
        val DEFAULT = ItemOptions(false)
        val STAR_PREFIX = ItemOptions(true)
    }
}

open data class ExpectedInfo(val type: JetType, val expectedName: String?, val tail: Tail?, val itemOptions: ItemOptions = ItemOptions.DEFAULT)

data class ArgumentPosition(val argumentIndex: Int, val argumentName: String?, val isFunctionLiteralArgument: Boolean) {
    constructor(argumentIndex: Int, isFunctionLiteralArgument: Boolean = false) : this(argumentIndex, null, isFunctionLiteralArgument)
    constructor(argumentIndex: Int, argumentName: String?) : this(argumentIndex, argumentName, false)
}

class ArgumentExpectedInfo(type: JetType, name: String?, tail: Tail?, val function: FunctionDescriptor, val position: ArgumentPosition, itemOptions: ItemOptions = ItemOptions.DEFAULT)
  : ExpectedInfo(type, name, tail, itemOptions) {

    override fun equals(other: Any?)
            = other is ArgumentExpectedInfo && super.equals(other) && function == other.function && position == other.position

    override fun hashCode()
            = function.hashCode()
}

class ReturnValueExpectedInfo(type: JetType, val callable: CallableDescriptor) : ExpectedInfo(type, callable.getName().asString(), null) {
    override fun equals(other: Any?)
            = other is ReturnValueExpectedInfo && super.equals(other) && callable == other.callable

    override fun hashCode()
            = callable.hashCode()
}


class ExpectedInfos(
        val bindingContext: BindingContext,
        val resolutionFacade: ResolutionFacade,
        val moduleDescriptor: ModuleDescriptor,
        val useHeuristicSignatures: Boolean
) {
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
        val argumentList = argument.getParent() as? JetValueArgumentList ?: return null
        val callElement = argumentList.getParent() as? JetCallElement ?: return null
        return calculateForArgument(callElement, argument)
    }

    private fun calculateForFunctionLiteralArgument(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val functionLiteralArgument = expressionWithType.getParent() as? JetFunctionLiteralArgument
        val callExpression = functionLiteralArgument?.getParent() as? JetCallExpression ?: return null
        val literalArgument = callExpression.getFunctionLiteralArguments().firstOrNull() ?: return null
        if (literalArgument.getArgumentExpression() != expressionWithType) return null
        return calculateForArgument(callExpression, literalArgument)
    }

    private fun calculateForArgument(callElement: JetCallElement, argument: ValueArgument): Collection<ExpectedInfo>? {
        val call = callElement.getCall(bindingContext) ?: return null
        return calculateForArgument(call, argument)
    }

    public fun calculateForArgument(call: Call, argument: ValueArgument): Collection<ExpectedInfo>? {
        val argumentIndex = call.getValueArguments().indexOf(argument)
        assert(argumentIndex >= 0)
        val argumentName = argument.getArgumentName()?.getReferenceExpression()?.getReferencedName()
        val isFunctionLiteralArgument = argument is FunctionLiteralArgument
        val argumentPosition = ArgumentPosition(argumentIndex, argumentName, isFunctionLiteralArgument)

        val callElement = call.getCallElement()
        val calleeExpression = call.getCalleeExpression()

        // leave only arguments before the current one
        val truncatedCall = object : DelegatingCall(call) {
            val arguments = call.getValueArguments().subList(0, argumentIndex)

            override fun getValueArguments() = arguments
            override fun getFunctionLiteralArguments() = emptyList<FunctionLiteralArgument>()
            override fun getValueArgumentList() = null
        }
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, calleeExpression] ?: return null //TODO: discuss it

        val expectedType = (callElement as? JetExpression)?.let { bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, it] } ?: TypeUtils.NO_EXPECTED_TYPE
        val dataFlowInfo = bindingContext.getDataFlowInfo(calleeExpression)
        val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace for completion")
        val context = BasicCallResolutionContext.create(bindingTrace, resolutionScope, truncatedCall, expectedType, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckValueArgumentsMode.ENABLED,
                                                        CompositeChecker(listOf()), SymbolUsageValidator.Empty, AdditionalTypeChecker.Composite(listOf()), false)
        val callResolutionContext = context.replaceCollectAllCandidates(true)
        val callResolver = InjectorForMacros(
                callElement.getProject(),
                resolutionFacade.findModuleDescriptor(callElement)
        ).getCallResolver()
        val results: OverloadResolutionResults<FunctionDescriptor> = callResolver.resolveFunctionCall(callResolutionContext)

        val expectedInfos = HashSet<ExpectedInfo>()
        for (candidate: ResolvedCall<FunctionDescriptor> in results.getAllCandidates()!!) {
            val status = candidate.getStatus()
            if (status == ResolutionStatus.RECEIVER_TYPE_ERROR || status == ResolutionStatus.RECEIVER_PRESENCE_ERROR) continue

            // check that all arguments before the current one matched
            if (!candidate.noErrorsInValueArguments()) continue

            val descriptor = candidate.getResultingDescriptor()
            val parameters = descriptor.getValueParameters()
            if (parameters.isEmpty()) continue

            val argumentToParameter = call.mapArgumentsToParameters(descriptor)
            val parameter = argumentToParameter[argument] ?: continue

            val thisReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidate.getDispatchReceiver(), bindingContext)
            if (!Visibilities.isVisible(thisReceiver, descriptor, resolutionScope.getContainingDeclaration())) continue

            val expectedName = if (descriptor.hasSynthesizedParameterNames()) null else parameter.getName().asString()

            val varargElementType = parameter.getVarargElementType()
            if (varargElementType != null) {
                if (isFunctionLiteralArgument) continue

                if (argumentName == null) {
                    expectedInfos.add(ArgumentExpectedInfo(varargElementType, expectedName?.unpluralize(), null, descriptor, argumentPosition))

                    if (argumentIndex == parameters.indexOf(parameter)) {
                        val tail = if (parameter == parameters.last()) Tail.RPARENTH else null
                        expectedInfos.add(ArgumentExpectedInfo(parameter.getType(), expectedName, tail, descriptor, argumentPosition, ItemOptions.STAR_PREFIX))
                    }
                }
                else {
                    val tail = namedArgumentTail(argumentToParameter, argumentName, descriptor)
                    expectedInfos.add(ArgumentExpectedInfo(varargElementType, expectedName?.unpluralize(), tail, descriptor, argumentPosition))
                    expectedInfos.add(ArgumentExpectedInfo(parameter.getType(), expectedName, tail, descriptor, argumentPosition, ItemOptions.STAR_PREFIX))
                }
            }
            else {
                val lastNonOptionalParam = parameters.lastOrNull { !it.hasDefaultValue() }

                fun needCommaForParameter(parameter: ValueParameterDescriptor): Boolean {
                    if (parameter.hasDefaultValue()) return false // parameter is optional
                    if (parameter.getVarargElementType() != null) return false // vararg arguments list can be empty
                    // last non-optional parameter of functional type can be placed outside parenthesis:
                    if (parameter == lastNonOptionalParam && KotlinBuiltIns.isFunctionOrExtensionFunctionType(parameter.getType())) return false
                    return true
                }

                val parameterType = if (useHeuristicSignatures)
                    HeuristicSignatures.correctedParameterType(descriptor, parameter, moduleDescriptor, callElement.getProject()) ?: parameter.getType()
                else
                    parameter.getType()

                if (isFunctionLiteralArgument) {
                    if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
                        expectedInfos.add(ArgumentExpectedInfo(parameterType, expectedName, null, descriptor, argumentPosition))
                    }
                }
                else {
                    val tail = if (argumentName == null) {
                        if (parameter == parameters.last())
                            Tail.RPARENTH //TODO: support square brackets
                        else if (parameters.dropWhile { it != parameter }.drop(1).none(::needCommaForParameter))
                            null
                        else
                            Tail.COMMA
                    }
                    else {
                        namedArgumentTail(argumentToParameter, argumentName, descriptor)
                    }

                    expectedInfos.add(ArgumentExpectedInfo(parameterType, expectedName, tail, descriptor, argumentPosition))
                }
            }
        }
        return expectedInfos
    }

    private fun namedArgumentTail(argumentToParameter: Map<ValueArgument, ValueParameterDescriptor>, argumentName: String, descriptor: FunctionDescriptor): Tail? {
        val usedParameterNames = (argumentToParameter.values().map { it.getName().asString() } + listOf(argumentName)).toSet()
        val notUsedParameters = descriptor.getValueParameters().filter { it.getName().asString() !in usedParameterNames }
        return if (notUsedParameters.isEmpty())
            Tail.RPARENTH
        else if (notUsedParameters.all { it.hasDefaultValue() })
            null
        else
            Tail.COMMA
    }

    private fun calculateForEqAndAssignment(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.getParent() as? JetBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.getOperationToken()
            if (operationToken == JetTokens.EQ || operationToken == JetTokens.EQEQ || operationToken == JetTokens.EXCLEQ
                || operationToken == JetTokens.EQEQEQ || operationToken == JetTokens.EXCLEQEQEQ) {
                val otherOperand = if (expressionWithType == binaryExpression.getRight()) binaryExpression.getLeft() else binaryExpression.getRight()
                if (otherOperand != null) {
                    val expressionType = bindingContext.getType(otherOperand) ?: return null
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

            ifExpression.getThen() -> calculate(ifExpression)?.map { ExpectedInfo(it.type, it.expectedName, Tail.ELSE) }

            ifExpression.getElse() -> {
                val ifExpectedInfo = calculate(ifExpression)
                val thenType = ifExpression.getThen()?.let { bindingContext.getType(it) }
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
                val leftType = bindingContext.getType(leftExpression)
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
        return calculate(block)?.map { ExpectedInfo(it.type, it.expectedName, null) }
    }

    private fun calculateForWhenEntryValue(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val condition = expressionWithType.getParent() as? JetWhenConditionWithExpression ?: return null
        val entry = condition.getParent() as JetWhenEntry
        val whenExpression = entry.getParent() as JetWhenExpression
        val subject = whenExpression.getSubjectExpression()
        if (subject != null) {
            val subjectType = bindingContext.getType(subject) ?: return null
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

    private fun functionReturnValueExpectedInfo(descriptor: FunctionDescriptor): ReturnValueExpectedInfo? {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> ReturnValueExpectedInfo(descriptor.getReturnType() ?: return null, descriptor)

            is PropertyGetterDescriptor -> {
                if (descriptor !is PropertyGetterDescriptor) return null
                val property = descriptor.getCorrespondingProperty()
                ReturnValueExpectedInfo(property.getType(), property)
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
            is JetArrayAccessExpression -> expectedNameFromExpression(expression.getArrayExpression())?.unpluralize()
            else -> null
        }
    }

    private fun String.unpluralize()
            = StringUtil.unpluralize(this)
}
