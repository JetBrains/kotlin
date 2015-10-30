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
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.core.mapArgumentsToParameters
import org.jetbrains.kotlin.idea.core.resolveCandidates
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.ideService
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.allArgumentsMapped
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

enum class Tail {
    COMMA,
    RPARENTH,
    RBRACKET,
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

    object None : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) = null
    }
}

class ByExpectedTypeFilter(val fuzzyType: FuzzyType) : ByTypeFilter {
    override fun matchingSubstitutor(descriptorType: FuzzyType) = descriptorType.checkIsSubtypeOf(fuzzyType)

    override fun equals(other: Any?) = other is ByExpectedTypeFilter && fuzzyType == other.fuzzyType

    override fun hashCode() = fuzzyType.hashCode()
}

data /* for copy() */
class ExpectedInfo(
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

    constructor(type: KotlinType, expectedName: String?, tail: Tail?, itemOptions: ItemOptions = ItemOptions.DEFAULT, additionalData: ExpectedInfo.AdditionalData? = null)
        : this(FuzzyType(type, emptyList()), expectedName, tail, itemOptions, additionalData)

    fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? = filter.matchingSubstitutor(descriptorType)

    fun matchingSubstitutor(descriptorType: KotlinType): TypeSubstitutor? = matchingSubstitutor(FuzzyType(descriptorType, emptyList()))

    companion object {
        fun createForArgument(type: KotlinType, expectedName: String?, tail: Tail?, argumentData: ArgumentPositionData, itemOptions: ItemOptions = ItemOptions.DEFAULT): ExpectedInfo {
            return ExpectedInfo(FuzzyType(type, argumentData.function.typeParameters), expectedName, tail, itemOptions, argumentData)
        }

        fun createForNamedArgumentExpected(argumentData: ArgumentPositionData): ExpectedInfo {
            return ExpectedInfo(ByTypeFilter.None, null, null/*TODO?*/, ItemOptions.DEFAULT, argumentData)
        }

        fun createForReturnValue(type: KotlinType?, callable: CallableDescriptor): ExpectedInfo {
            val filter = if (type != null) ByExpectedTypeFilter(FuzzyType(type, emptyList())) else ByTypeFilter.All
            return ExpectedInfo(filter, callable.name.asString(), null, additionalData = ReturnValueAdditionalData(callable))
        }
    }
}

val ExpectedInfo.fuzzyType: FuzzyType?
    get() = (this.filter as? ByExpectedTypeFilter)?.fuzzyType

sealed class ArgumentPositionData(val function: FunctionDescriptor, val callType: Call.CallType) : ExpectedInfo.AdditionalData {
    class Positional(
            function: FunctionDescriptor,
            callType: Call.CallType,
            val argumentIndex: Int,
            val isFunctionLiteralArgument: Boolean,
            val namedArgumentCandidates: Collection<ParameterDescriptor>
    ) : ArgumentPositionData(function, callType)

    class Named(function: FunctionDescriptor, callType: Call.CallType, val argumentName: Name) : ArgumentPositionData(function, callType)
}

class ReturnValueAdditionalData(val callable: CallableDescriptor) : ExpectedInfo.AdditionalData

class WhenEntryAdditionalData(val whenWithSubject: Boolean) : ExpectedInfo.AdditionalData

object IfConditionAdditionalData : ExpectedInfo.AdditionalData

class ExpectedInfos(
        val bindingContext: BindingContext,
        val resolutionFacade: ResolutionFacade,
        val useHeuristicSignatures: Boolean = true,
        val useOuterCallsExpectedTypeCount: Int = 0
) {
    public fun calculate(expressionWithType: KtExpression): Collection<ExpectedInfo> {
        val expectedInfos = calculateForArgument(expressionWithType)
                            ?: calculateForFunctionLiteralArgument(expressionWithType)
                            ?: calculateForIndexingArgument(expressionWithType)
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

    private fun calculateForArgument(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val argument = expressionWithType.getParent() as? KtValueArgument ?: return null
        val argumentList = argument.getParent() as? KtValueArgumentList ?: return null
        val callElement = argumentList.getParent() as? KtCallElement ?: return null
        return calculateForArgument(callElement, argument)
    }

    private fun calculateForFunctionLiteralArgument(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val functionLiteralArgument = expressionWithType.getParent() as? KtFunctionLiteralArgument
        val callExpression = functionLiteralArgument?.getParent() as? KtCallExpression ?: return null
        val literalArgument = callExpression.getFunctionLiteralArguments().firstOrNull() ?: return null
        if (literalArgument.getArgumentExpression() != expressionWithType) return null
        return calculateForArgument(callExpression, literalArgument)
    }

    private fun calculateForIndexingArgument(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val containerNode = expressionWithType.parent as? KtContainerNode ?: return null
        val arrayAccessExpression = containerNode.parent as? KtArrayAccessExpression ?: return null
        if (containerNode != arrayAccessExpression.indicesNode) return null
        val call = arrayAccessExpression.getCall(bindingContext) ?: return null
        val argument = call.valueArguments.firstOrNull { it.getArgumentExpression() == expressionWithType } ?: return null
        return calculateForArgument(call, argument)
    }

    private fun calculateForArgument(callElement: KtCallElement, argument: ValueArgument): Collection<ExpectedInfo>? {
        val call = callElement.getCall(bindingContext) ?: return null
        return calculateForArgument(call, argument)
    }

    public fun calculateForArgument(call: Call, argument: ValueArgument): Collection<ExpectedInfo> {
        val results = calculateForArgument(call, TypeUtils.NO_EXPECTED_TYPE, argument)

        fun makesSenseToUseOuterCallExpectedType(info: ExpectedInfo): Boolean {
            val data = info.additionalData as ArgumentPositionData
            return info.fuzzyType != null
                   && info.fuzzyType!!.freeParameters.isNotEmpty()
                   && data.function.fuzzyReturnType()?.freeParameters?.isNotEmpty() ?: false
        }

        if (useOuterCallsExpectedTypeCount > 0 && results.any(::makesSenseToUseOuterCallExpectedType)) {
            val callExpression = (call.callElement as? KtExpression)?.getQualifiedExpressionForSelectorOrThis() ?: return results
            val expectedFuzzyTypes = ExpectedInfos(bindingContext, resolutionFacade, useHeuristicSignatures, useOuterCallsExpectedTypeCount - 1)
                    .calculate(callExpression)
                    .map { it.fuzzyType }
                    .filterNotNull()
            if (expectedFuzzyTypes.isEmpty() || expectedFuzzyTypes.any { it.freeParameters.isNotEmpty() }) return results

            return expectedFuzzyTypes
                    .map { it.type }
                    .toSet()
                    .flatMap { calculateForArgument(call, it, argument) }
        }

        return results
    }

    private fun calculateForArgument(call: Call, callExpectedType: KotlinType, argument: ValueArgument): Collection<ExpectedInfo> {
        val argumentIndex = call.getValueArguments().indexOf(argument)
        assert(argumentIndex >= 0) {
            "Could not find argument '$argument(${argument.asElement().text})' among arguments of call: $call"
        }

        // leave only arguments before the current one
        val truncatedCall = object : DelegatingCall(call) {
            val arguments = call.getValueArguments().subList(0, argumentIndex)

            override fun getValueArguments() = arguments
            override fun getFunctionLiteralArguments() = emptyList<FunctionLiteralArgument>()
            override fun getValueArgumentList() = null
        }

        val candidates = truncatedCall.resolveCandidates(bindingContext, resolutionFacade, callExpectedType)

        val expectedInfos = ArrayList<ExpectedInfo>()

        for (candidate in candidates) {
            expectedInfos.addExpectedInfoForCandidate(candidate, call, argument, argumentIndex, checkPrevArgumentsMatched = true)
        }

        if (expectedInfos.isEmpty()) { // if no candidates have previous arguments matched, try with no type checking for them
            for (candidate in candidates) {
                expectedInfos.addExpectedInfoForCandidate(candidate, call, argument, argumentIndex, checkPrevArgumentsMatched = false)
            }
        }

        return expectedInfos
    }

    private fun MutableCollection<ExpectedInfo>.addExpectedInfoForCandidate(
            candidate: ResolvedCall<FunctionDescriptor>,
            call: Call,
            argument: ValueArgument,
            argumentIndex: Int,
            checkPrevArgumentsMatched: Boolean
    ) {
        // check that all arguments before the current has mappings to parameters
        if (!candidate.allArgumentsMapped()) return

        // check that all arguments before the current one matched
        if (checkPrevArgumentsMatched && !candidate.allArgumentsMatched()) return

        var descriptor = candidate.getResultingDescriptor()
        if (descriptor.valueParameters.isEmpty()) return

        var argumentToParameter = call.mapArgumentsToParameters(descriptor)
        var parameter = argumentToParameter[argument]

        //TODO: we can loose partially inferred substitution here but what to do?
        if (parameter != null && parameter.type.containsError()) {
            descriptor = descriptor.original
            parameter = descriptor.valueParameters[parameter.index]!!
            argumentToParameter = call.mapArgumentsToParameters(descriptor)
        }

        val argumentName = argument.getArgumentName()?.asName
        val isFunctionLiteralArgument = argument is FunctionLiteralArgument

        val callType = call.callType
        val isArrayAccess = callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD
        val rparenthTail = if (isArrayAccess) Tail.RBRACKET else Tail.RPARENTH

        val argumentPositionData = if (argumentName != null) {
            ArgumentPositionData.Named(descriptor, callType, argumentName)
        }
        else {
            val namedArgumentCandidates = if (!isFunctionLiteralArgument && !isArrayAccess && descriptor.hasStableParameterNames()) {
                val usedParameters = argumentToParameter.filter { it.key != argument }.map { it.value }.toSet()
                descriptor.valueParameters.filter { it !in usedParameters }
            }
            else {
                emptyList()
            }
            ArgumentPositionData.Positional(descriptor, callType, argumentIndex, isFunctionLiteralArgument, namedArgumentCandidates)
        }

        var parameters = descriptor.valueParameters
        if (callType == Call.CallType.ARRAY_SET_METHOD) { // last parameter in set is used for value assigned
            if (parameter == parameters.last()) {
                parameter = null
            }
            parameters = parameters.dropLast(1)
        }

        if (parameter == null) {
            if (argumentPositionData is ArgumentPositionData.Positional && argumentPositionData.namedArgumentCandidates.isNotEmpty()) {
                add(ExpectedInfo.createForNamedArgumentExpected(argumentPositionData))
            }
            return
        }

        val expectedName = if (descriptor.hasSynthesizedParameterNames()) null else parameter.getName().asString()

        fun needCommaForParameter(parameter: ValueParameterDescriptor): Boolean {
            if (parameter.hasDefaultValue()) return false // parameter is optional
            if (parameter.getVarargElementType() != null) return false // vararg arguments list can be empty
            // last parameter of functional type can be placed outside parenthesis:
            if (!isArrayAccess && parameter == parameters.last() && KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameter.getType())) return false
            return true
        }

        val tail = if (argumentName == null) {
            if (parameter == parameters.last())
                rparenthTail
            else if (parameters.dropWhile { it != parameter }.drop(1).any(::needCommaForParameter))
                Tail.COMMA
            else
                null
        }
        else {
            namedArgumentTail(argumentToParameter, argumentName, descriptor)
        }

        val alreadyHasStar = argument.getSpreadElement() != null

        val varargElementType = parameter!!.getVarargElementType()
        if (varargElementType != null) {
            if (isFunctionLiteralArgument) return

            val varargTail = if (argumentName == null && tail == rparenthTail)
                null /* even if it's the last parameter, there can be more arguments for the same parameter */
            else
                tail

            if (!alreadyHasStar) {
                add(ExpectedInfo.createForArgument(varargElementType, expectedName?.unpluralize(), varargTail, argumentPositionData))
            }

            val starOptions = if (!alreadyHasStar) ItemOptions.STAR_PREFIX else ItemOptions.DEFAULT
            add(ExpectedInfo.createForArgument(parameter!!.getType(), expectedName, varargTail, argumentPositionData, starOptions))
        }
        else {
            if (alreadyHasStar) return

            val parameterType = if (useHeuristicSignatures)
                resolutionFacade.ideService<HeuristicSignatures>().
                        correctedParameterType(descriptor, parameter!!) ?: parameter!!.getType()
            else
                parameter!!.getType()

            if (isFunctionLiteralArgument) {
                if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
                    add(ExpectedInfo.createForArgument(parameterType, expectedName, null, argumentPositionData))
                }
            }
            else {
                add(ExpectedInfo.createForArgument(parameterType, expectedName, tail, argumentPositionData))
            }
        }
    }

    private fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMatched()
            = call.valueArguments.none { argument -> getArgumentMapping(argument).isError() && !argument.hasError() /* ignore arguments that has error type */ }

    private fun ValueArgument.hasError()
            = getArgumentExpression()?.let { bindingContext.getType(it) }?.isError ?: true

    private fun namedArgumentTail(argumentToParameter: Map<ValueArgument, ValueParameterDescriptor>, argumentName: Name, descriptor: FunctionDescriptor): Tail? {
        val usedParameterNames = (argumentToParameter.values().map { it.getName() } + listOf(argumentName)).toSet()
        val notUsedParameters = descriptor.getValueParameters().filter { it.getName() !in usedParameterNames }
        return if (notUsedParameters.isEmpty())
            Tail.RPARENTH // named arguments no supported for []
        else if (notUsedParameters.all { it.hasDefaultValue() })
            null
        else
            Tail.COMMA
    }

    private fun calculateForEqAndAssignment(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.getParent() as? KtBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.getOperationToken()
            if (operationToken == KtTokens.EQ || operationToken in COMPARISON_TOKENS) {
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

    private fun calculateForIf(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val ifExpression = (expressionWithType.getParent() as? KtContainerNode)?.getParent() as? KtIfExpression ?: return null
        return when (expressionWithType) {
            ifExpression.getCondition() -> listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.booleanType, null, Tail.RPARENTH, additionalData = IfConditionAdditionalData))

            ifExpression.getThen() -> calculate(ifExpression).map { ExpectedInfo(it.filter, it.expectedName, Tail.ELSE) }

            ifExpression.getElse() -> {
                val ifExpectedInfo = calculate(ifExpression)
                val thenType = ifExpression.getThen()?.let { bindingContext.getType(it) }
                val filteredInfo = if (thenType != null && !thenType.isError())
                    ifExpectedInfo.filter { it.matchingSubstitutor(thenType) != null }
                else
                    ifExpectedInfo
                return filteredInfo.copyWithNoAdditionalData()
            }

            else -> return null
        }
    }

    private fun calculateForElvis(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.getParent() as? KtBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.getOperationToken()
            if (operationToken == KtTokens.ELVIS && expressionWithType == binaryExpression.getRight()) {
                val leftExpression = binaryExpression.getLeft() ?: return null
                val leftType = bindingContext.getType(leftExpression)
                val leftTypeNotNullable = leftType?.makeNotNullable()
                val expectedInfos = calculate(binaryExpression)
                if (expectedInfos.isNotEmpty()) {
                    val filteredInfo = if (leftTypeNotNullable != null)
                        expectedInfos.filter { it.matchingSubstitutor(leftTypeNotNullable) != null }
                    else
                        expectedInfos
                    return filteredInfo.copyWithNoAdditionalData()
                }
                else if (leftTypeNotNullable != null) {
                    return listOf(ExpectedInfo(leftTypeNotNullable, null, null))
                }
            }
        }
        return null
    }

    private fun calculateForBlockExpression(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val block = expressionWithType.parent as? KtBlockExpression ?: return null
        if (expressionWithType != block.statements.last()) return null

        val functionLiteral = block.parent as? KtFunctionLiteral
        if (functionLiteral != null) {
            val literalExpression = functionLiteral.parent as KtFunctionLiteralExpression
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

    private fun calculateForWhenEntryValue(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val condition = expressionWithType.getParent() as? KtWhenConditionWithExpression ?: return null
        val entry = condition.getParent() as KtWhenEntry
        val whenExpression = entry.getParent() as KtWhenExpression
        val subject = whenExpression.getSubjectExpression()
        if (subject != null) {
            val subjectType = bindingContext.getType(subject) ?: return null
            return listOf(ExpectedInfo(subjectType, null, null, additionalData = WhenEntryAdditionalData(whenWithSubject = true)))
        }
        else {
            return listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.booleanType, null, null, additionalData = WhenEntryAdditionalData(whenWithSubject = false)))
        }
    }

    private fun calculateForExclOperand(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val prefixExpression = expressionWithType.getParent() as? KtPrefixExpression ?: return null
        if (prefixExpression.getOperationToken() != KtTokens.EXCL) return null
        return listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.booleanType, null, null))
    }

    private fun calculateForInitializer(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val property = expressionWithType.getParent() as? KtProperty ?: return null
        if (expressionWithType != property.getInitializer()) return null
        val propertyDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? VariableDescriptor ?: return null
        val expectedName = propertyDescriptor.name.asString()
        val expectedInfo = if (property.typeReference != null)
            ExpectedInfo(propertyDescriptor.getType(), expectedName, null)
        else
            ExpectedInfo(ByTypeFilter.All, expectedName, null) // no explicit type - only expected name known
        return listOf(expectedInfo)
    }

    private fun calculateForExpressionBody(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val declaration = expressionWithType.getParent() as? KtDeclarationWithBody ?: return null
        if (expressionWithType != declaration.getBodyExpression() || declaration.hasBlockBody()) return null
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? FunctionDescriptor ?: return null
        return functionReturnValueExpectedInfo(descriptor, expectType = declaration.hasDeclaredReturnType()).singletonOrEmptyList()
    }

    private fun calculateForReturn(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val returnExpression = expressionWithType.getParent() as? KtReturnExpression ?: return null
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

    private fun calculateForLoopRange(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val forExpression = (expressionWithType.parent as? KtContainerNode)
                                    ?.parent as? KtForExpression ?: return null
        if (expressionWithType != forExpression.loopRange) return null

        val loopVar = forExpression.loopParameter
        val loopVarType = if (loopVar != null && loopVar.typeReference != null)
            (resolutionFacade.resolveToDescriptor(loopVar) as VariableDescriptor).type.check { !it.isError }
        else
            null

        val scope = expressionWithType.getResolutionScope(bindingContext, resolutionFacade)
        val iterableDetector = resolutionFacade.ideService<IterableTypesDetection>().createDetector(scope)

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? {
                return if (iterableDetector.isIterable(descriptorType, loopVarType)) TypeSubstitutor.EMPTY else null
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, Tail.RPARENTH))
    }

    private fun calculateForInOperatorArgument(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? KtBinaryExpression ?: return null
        val operationToken = binaryExpression.operationToken
        if (operationToken != KtTokens.IN_KEYWORD && operationToken != KtTokens.NOT_IN || expressionWithType != binaryExpression.right) return null

        val leftOperandType = binaryExpression.left?.let { bindingContext.getType(it) } ?: return null
        val scope = expressionWithType.getResolutionScope(bindingContext, resolutionFacade)
        val detector = TypesWithContainsDetector(scope, leftOperandType, resolutionFacade)

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? {
                return if (detector.hasContains(descriptorType)) TypeSubstitutor.EMPTY else null
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, null))
    }

    private fun getFromBindingContext(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
        return listOf(ExpectedInfo(expectedType, null, null))
    }

    private fun expectedNameFromExpression(expression: KtExpression?): String? {
        return when (expression) {
            is KtSimpleNameExpression -> expression.getReferencedName()
            is KtQualifiedExpression -> expectedNameFromExpression(expression.getSelectorExpression())
            is KtCallExpression -> expectedNameFromExpression(expression.getCalleeExpression())
            is KtArrayAccessExpression -> expectedNameFromExpression(expression.getArrayExpression())?.unpluralize()
            else -> null
        }
    }

    private fun String.unpluralize()
            = StringUtil.unpluralize(this)

    private fun Collection<ExpectedInfo>.copyWithNoAdditionalData() = map { it.copy(additionalData = null, itemOptions = ItemOptions.DEFAULT) }
}

val COMPARISON_TOKENS = setOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)

