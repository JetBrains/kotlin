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
import org.jetbrains.kotlin.idea.completion.smart.TypesWithContainsDetector
import org.jetbrains.kotlin.idea.core.IterableTypesDetection
import org.jetbrains.kotlin.idea.core.mapArgumentsToParameters
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.resolve.ideService
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.noErrorsInValueArguments
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.LinkedHashSet

enum class Tail {
    COMMA,
    RPARENTH,
    ELSE,
    RBRACE
}

data class ItemOptions(val starPrefix: Boolean) {
    companion object {
        val DEFAULT = ItemOptions(false)
        val STAR_PREFIX = ItemOptions(true)
    }
}

interface ByTypeFilter {
    fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor?

    object All : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) = TypeSubstitutor.EMPTY
    }
}

class ByExpectedTypeFilter(val fuzzyType: FuzzyType) : ByTypeFilter {
    override fun matchingSubstitutor(descriptorType: FuzzyType) = descriptorType.checkIsSubtypeOf(fuzzyType)

    override fun equals(other: Any?) = other is ByExpectedTypeFilter && fuzzyType == other.fuzzyType

    override fun hashCode() = fuzzyType.hashCode()
}

data class ExpectedInfo(
        val filter: ByTypeFilter,
        val expectedName: String?,
        val tail: Tail?,
        val itemOptions: ItemOptions = ItemOptions.DEFAULT,
        val additionalData: ExpectedInfo.AdditionalData? = null
) {
    // just a marker interface
    interface AdditionalData {}

    constructor(fuzzyType: FuzzyType, expectedName: String?, tail: Tail?, itemOptions: ItemOptions = ItemOptions.DEFAULT, additionalData: ExpectedInfo.AdditionalData? = null)
        : this(ByExpectedTypeFilter(fuzzyType), expectedName, tail, itemOptions, additionalData)

    constructor(type: JetType, expectedName: String?, tail: Tail?, itemOptions: ItemOptions = ItemOptions.DEFAULT, additionalData: ExpectedInfo.AdditionalData? = null)
        : this(FuzzyType(type, emptyList()), expectedName, tail, itemOptions, additionalData)

    fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? = filter.matchingSubstitutor(descriptorType)

    fun matchingSubstitutor(descriptorType: JetType): TypeSubstitutor? = matchingSubstitutor(FuzzyType(descriptorType, emptyList()))

    companion object {
        fun createForArgument(type: JetType, name: String?, tail: Tail?, function: FunctionDescriptor, position: ArgumentPosition, itemOptions: ItemOptions = ItemOptions.DEFAULT): ExpectedInfo {
            return ExpectedInfo(FuzzyType(type, function.typeParameters), name, tail, itemOptions, ArgumentAdditionalData(function, position))
        }

        fun createForReturnValue(type: JetType?, callable: CallableDescriptor): ExpectedInfo {
            val filter = if (type != null) ByExpectedTypeFilter(FuzzyType(type, emptyList())) else ByTypeFilter.All
            return ExpectedInfo(filter, callable.name.asString(), null, additionalData = ReturnValueAdditionalData(callable))
        }
    }
}

val ExpectedInfo.fuzzyType: FuzzyType?
    get() = (this.filter as? ByExpectedTypeFilter)?.fuzzyType

data class ArgumentPosition(val argumentIndex: Int, val argumentName: Name?, val isFunctionLiteralArgument: Boolean) {
    constructor(argumentIndex: Int, isFunctionLiteralArgument: Boolean = false) : this(argumentIndex, null, isFunctionLiteralArgument)
    constructor(argumentIndex: Int, argumentName: Name?) : this(argumentIndex, argumentName, false)
}

class ArgumentAdditionalData(val function: FunctionDescriptor, val position: ArgumentPosition) : ExpectedInfo.AdditionalData {
    override fun equals(other: Any?)
            = other is ArgumentAdditionalData && function == other.function && position == other.position

    override fun hashCode()
            = function.hashCode()
}

class ReturnValueAdditionalData(val callable: CallableDescriptor) : ExpectedInfo.AdditionalData {
    override fun equals(other: Any?)
            = other is ReturnValueAdditionalData && callable == other.callable

    override fun hashCode()
            = callable.hashCode()
}

class ExpectedInfos(
        val bindingContext: BindingContext,
        val resolutionFacade: ResolutionFacade,
        val useHeuristicSignatures: Boolean = true,
        val useOuterCallsExpectedTypeCount: Int = 0
) {
    public fun calculate(expressionWithType: JetExpression): Collection<ExpectedInfo> {
        val expectedInfos = calculateForArgument(expressionWithType)
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
                            ?: calculateForLoopRange(expressionWithType)
                            ?: calculateForInOperatorArgument(expressionWithType)
                            ?: getFromBindingContext(expressionWithType)
                            ?: return emptyList()
        return expectedInfos.filterNot { it.fuzzyType?.type?.isError ?: false }
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

    public fun calculateForArgument(call: Call, argument: ValueArgument): Collection<ExpectedInfo> {
        val results = calculateForArgument(call, TypeUtils.NO_EXPECTED_TYPE, argument)

        fun makesSenseToUseOuterCallExpectedType(info: ExpectedInfo): Boolean {
            val data = info.additionalData as ArgumentAdditionalData
            return info.fuzzyType != null
                   && info.fuzzyType!!.freeParameters.isNotEmpty()
                   && data.function.fuzzyReturnType()?.freeParameters?.isNotEmpty() ?: false
        }

        if (useOuterCallsExpectedTypeCount > 0 && results.any(::makesSenseToUseOuterCallExpectedType)) {
            val callExpression = (call.callElement as? JetExpression)?.getQualifiedExpressionForSelectorOrThis() ?: return results
            val expectedFuzzyTypes = ExpectedInfos(bindingContext, resolutionFacade, useHeuristicSignatures, useOuterCallsExpectedTypeCount - 1)
                    .calculate(callExpression)
                    .map { it.fuzzyType }
                    .filterNotNull()
            if (expectedFuzzyTypes.isEmpty() || expectedFuzzyTypes.any { it.freeParameters.isNotEmpty() }) return results

            return expectedFuzzyTypes
                    .map { it.type }
                    .toSet()
                    .flatMap { calculateForArgument(call, it, argument) }
                    .toSet()
        }

        return results
    }

    private fun calculateForArgument(call: Call, callExpectedType: JetType, argument: ValueArgument): Collection<ExpectedInfo> {
        val argumentIndex = call.getValueArguments().indexOf(argument)
        assert(argumentIndex >= 0) {
            "Could not find argument '$argument(${argument.asElement().text})' among arguments of call: $call"
        }
        val argumentName = argument.getArgumentName()?.asName
        val isFunctionLiteralArgument = argument is FunctionLiteralArgument
        val argumentPosition = ArgumentPosition(argumentIndex, argumentName, isFunctionLiteralArgument)

        // leave only arguments before the current one
        val truncatedCall = object : DelegatingCall(call) {
            val arguments = call.getValueArguments().subList(0, argumentIndex)

            override fun getValueArguments() = arguments
            override fun getFunctionLiteralArguments() = emptyList<FunctionLiteralArgument>()
            override fun getValueArgumentList() = null
        }
        val resolutionScope = bindingContext[BindingContext.LEXICAL_SCOPE, call.calleeExpression] ?: return emptyList() //TODO: discuss it

        val dataFlowInfo = bindingContext.getDataFlowInfo(call.calleeExpression)
        val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace for completion")
        val context = BasicCallResolutionContext.create(bindingTrace, resolutionScope, truncatedCall, callExpectedType, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                        CallChecker.DoNothing, false)
        val callResolutionContext = context.replaceCollectAllCandidates(true)
        val callResolver = resolutionFacade.frontendService<CallResolver>()
        val results: OverloadResolutionResults<FunctionDescriptor> = callResolver.resolveFunctionCall(callResolutionContext)

        val expectedInfos = LinkedHashSet<ExpectedInfo>()
        for (candidate: ResolvedCall<FunctionDescriptor> in results.getAllCandidates()!!) {
            val status = candidate.getStatus()
            if (status == ResolutionStatus.RECEIVER_TYPE_ERROR || status == ResolutionStatus.RECEIVER_PRESENCE_ERROR) continue

            // check that all arguments before the current one matched
            if (!candidate.noErrorsInValueArguments()) continue

            var descriptor = candidate.getResultingDescriptor()
            if (descriptor.valueParameters.isEmpty()) continue

            val thisReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidate.getDispatchReceiver(), bindingContext)
            if (!Visibilities.isVisible(thisReceiver, descriptor, resolutionScope.ownerDescriptor)) continue

            var argumentToParameter = call.mapArgumentsToParameters(descriptor)
            var parameter = argumentToParameter[argument] ?: continue

            //TODO: we can loose partially inferred substitution here but what to do?
            if (parameter.type.containsError()) {
                descriptor = descriptor.original
                parameter = descriptor.valueParameters[parameter.index]
                argumentToParameter = call.mapArgumentsToParameters(descriptor)
            }

            val expectedName = if (descriptor.hasSynthesizedParameterNames()) null else parameter.getName().asString()

            val parameters = descriptor.valueParameters

            fun needCommaForParameter(parameter: ValueParameterDescriptor): Boolean {
                if (parameter.hasDefaultValue()) return false // parameter is optional
                if (parameter.getVarargElementType() != null) return false // vararg arguments list can be empty
                // last parameter of functional type can be placed outside parenthesis:
                if (parameter == parameters.last() && KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameter.getType())) return false
                return true
            }

            val tail = if (argumentName == null) {
                if (parameter == parameters.last())
                    Tail.RPARENTH //TODO: support square brackets
                else if (parameters.dropWhile { it != parameter }.drop(1).any(::needCommaForParameter))
                    Tail.COMMA
                else
                    null
            }
            else {
                namedArgumentTail(argumentToParameter, argumentName, descriptor)
            }

            val alreadyHasStar = argument.getSpreadElement() != null

            val varargElementType = parameter.getVarargElementType()
            if (varargElementType != null) {
                if (isFunctionLiteralArgument) continue

                val varargTail = if (argumentName == null && tail == Tail.RPARENTH)
                    null /* even if it's the last parameter, there can be more arguments for the same parameter */
                else
                    tail

                if (!alreadyHasStar) {
                    expectedInfos.add(ExpectedInfo.createForArgument(varargElementType, expectedName?.unpluralize(), varargTail, descriptor, argumentPosition))
                }

                val starOptions = if (!alreadyHasStar) ItemOptions.STAR_PREFIX else ItemOptions.DEFAULT
                expectedInfos.add(ExpectedInfo.createForArgument(parameter.getType(), expectedName, varargTail, descriptor, argumentPosition, starOptions))
            }
            else {
                if (alreadyHasStar) continue

                val parameterType = if (useHeuristicSignatures)
                    resolutionFacade.ideService<HeuristicSignatures>().
                            correctedParameterType(descriptor, parameter) ?: parameter.getType()
                else
                    parameter.getType()

                if (isFunctionLiteralArgument) {
                    if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
                        expectedInfos.add(ExpectedInfo.createForArgument(parameterType, expectedName, null, descriptor, argumentPosition))
                    }
                }
                else {
                    expectedInfos.add(ExpectedInfo.createForArgument(parameterType, expectedName, tail, descriptor, argumentPosition))
                }
            }
        }
        return expectedInfos
    }

    private fun namedArgumentTail(argumentToParameter: Map<ValueArgument, ValueParameterDescriptor>, argumentName: Name, descriptor: FunctionDescriptor): Tail? {
        val usedParameterNames = (argumentToParameter.values().map { it.getName() } + listOf(argumentName)).toSet()
        val notUsedParameters = descriptor.getValueParameters().filter { it.getName() !in usedParameterNames }
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
            if (operationToken == JetTokens.EQ || operationToken in COMPARISON_TOKENS) {
                val otherOperand = if (expressionWithType == binaryExpression.getRight()) binaryExpression.getLeft() else binaryExpression.getRight()
                if (otherOperand != null) {
                    var expectedType = bindingContext.getType(otherOperand) ?: return null

                    // if we complete argument of == or !=, make types in expected info's nullable to allow nullable items too
                    if (operationToken in COMPARISON_TOKENS) {
                        expectedType = expectedType.makeNullable()
                    }

                    return listOf(ExpectedInfo(expectedType, expectedNameFromExpression(otherOperand), null))
                }
            }
        }
        return null
    }

    private fun calculateForIf(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val ifExpression = (expressionWithType.getParent() as? JetContainerNode)?.getParent() as? JetIfExpression ?: return null
        return when (expressionWithType) {
            ifExpression.getCondition() -> listOf(ExpectedInfo(KotlinBuiltIns.getInstance().getBooleanType(), null, Tail.RPARENTH))

            ifExpression.getThen() -> calculate(ifExpression).map { ExpectedInfo(it.filter, it.expectedName, Tail.ELSE) }

            ifExpression.getElse() -> {
                val ifExpectedInfo = calculate(ifExpression)
                val thenType = ifExpression.getThen()?.let { bindingContext.getType(it) }
                if (thenType != null && !thenType.isError())
                    ifExpectedInfo.filter { it.matchingSubstitutor(thenType) != null }
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
                if (expectedInfos.isNotEmpty()) {
                    return if (leftTypeNotNullable != null)
                        expectedInfos.filter { it.matchingSubstitutor(leftTypeNotNullable) != null }
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
        val block = expressionWithType.parent as? JetBlockExpression ?: return null
        if (expressionWithType != block.statements.last()) return null

        val functionLiteral = block.parent as? JetFunctionLiteral
        if (functionLiteral != null) {
            val literalExpression = functionLiteral.parent as JetFunctionLiteralExpression
            return calculate(literalExpression)
                    .map { it.fuzzyType }
                    .filterNotNull()
                    .filter { KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(it.type) }
                    .map {
                        val returnType = KotlinBuiltIns.getReturnTypeFromFunctionType(it.type)
                        ExpectedInfo(FuzzyType(returnType, it.freeParameters), null, Tail.RBRACE)
                    }
        }
        else {
            return calculate(block).map { ExpectedInfo(it.filter, it.expectedName, null) }
        }
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
        val expectedName = propertyDescriptor.name.asString()
        val expectedInfo = if (property.typeReference != null)
            ExpectedInfo(propertyDescriptor.getType(), expectedName, null)
        else
            ExpectedInfo(ByTypeFilter.All, expectedName, null) // no explicit type - only expected name known
        return listOf(expectedInfo)
    }

    private fun calculateForExpressionBody(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val declaration = expressionWithType.getParent() as? JetDeclarationWithBody ?: return null
        if (expressionWithType != declaration.getBodyExpression() || declaration.hasBlockBody()) return null
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? FunctionDescriptor ?: return null
        return functionReturnValueExpectedInfo(descriptor, expectType = declaration.hasDeclaredReturnType()).singletonOrEmptyList()
    }

    private fun calculateForReturn(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val returnExpression = expressionWithType.getParent() as? JetReturnExpression ?: return null
        val descriptor = returnExpression.getTargetFunctionDescriptor(bindingContext) ?: return null
        return functionReturnValueExpectedInfo(descriptor, expectType = true).singletonOrEmptyList()
    }

    private fun functionReturnValueExpectedInfo(descriptor: FunctionDescriptor, expectType: Boolean): ExpectedInfo? {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> {
                val expectedType = if (expectType) descriptor.returnType else null
                ExpectedInfo.createForReturnValue(expectedType, descriptor)
            }

            is PropertyGetterDescriptor -> {
                if (descriptor !is PropertyGetterDescriptor) return null
                val property = descriptor.getCorrespondingProperty()
                val expectedType = if (expectType) property.type else null
                ExpectedInfo.createForReturnValue(expectedType, property)
            }

            else -> null
        }
    }

    private fun calculateForLoopRange(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val forExpression = (expressionWithType.parent as? JetContainerNode)
                                    ?.parent as? JetForExpression ?: return null
        if (expressionWithType != forExpression.loopRange) return null

        val loopVar = forExpression.loopParameter
        val loopVarType = if (loopVar != null && loopVar.typeReference != null)
            (resolutionFacade.resolveToDescriptor(loopVar) as VariableDescriptor).type.check { !it.isError }
        else
            null

        val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expressionWithType)!!
        val iterableDetector = resolutionFacade.ideService<IterableTypesDetection>().createDetector(scope)

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? {
                return if (iterableDetector.isIterable(descriptorType, loopVarType)) TypeSubstitutor.EMPTY else null
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, Tail.RPARENTH))
    }

    private fun calculateForInOperatorArgument(expressionWithType: JetExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? JetBinaryExpression ?: return null
        val operationToken = binaryExpression.operationToken
        if (operationToken != JetTokens.IN_KEYWORD && operationToken != JetTokens.NOT_IN || expressionWithType != binaryExpression.right) return null

        val leftOperandType = binaryExpression.left?.let { bindingContext.getType(it) } ?: return null
        val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expressionWithType)!!
        val detector = TypesWithContainsDetector(scope, leftOperandType, resolutionFacade.ideService<HeuristicSignatures>())

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? {
                return if (detector.hasContains(descriptorType)) TypeSubstitutor.EMPTY else null
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, null))
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

val COMPARISON_TOKENS = setOf(JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ)

