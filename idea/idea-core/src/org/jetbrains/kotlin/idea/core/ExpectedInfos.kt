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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.ideService
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.callUtil.allArgumentsMapped
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
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

    val fuzzyType: FuzzyType?
        get() = null

    val multipleFuzzyTypes: Collection<FuzzyType>
        get() = listOfNotNull(fuzzyType)

    object All : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) = TypeSubstitutor.EMPTY
    }

    object None : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) = null
    }
}

class ByExpectedTypeFilter(override val fuzzyType: FuzzyType) : ByTypeFilter {
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
        : this(type.toFuzzyType(emptyList()), expectedName, tail, itemOptions, additionalData)

    fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? = filter.matchingSubstitutor(descriptorType)

    fun matchingSubstitutor(descriptorType: KotlinType): TypeSubstitutor? = matchingSubstitutor(descriptorType.toFuzzyType(emptyList()))

    companion object {
        fun createForArgument(type: KotlinType, expectedName: String?, tail: Tail?, argumentData: ArgumentPositionData, itemOptions: ItemOptions = ItemOptions.DEFAULT): ExpectedInfo {
            return ExpectedInfo(type.toFuzzyType(argumentData.function.typeParameters), expectedName, tail, itemOptions, argumentData)
        }

        fun createForNamedArgumentExpected(argumentData: ArgumentPositionData): ExpectedInfo {
            return ExpectedInfo(ByTypeFilter.None, null, null/*TODO?*/, ItemOptions.DEFAULT, argumentData)
        }

        fun createForReturnValue(type: KotlinType?, callable: CallableDescriptor): ExpectedInfo {
            val filter = if (type != null) ByExpectedTypeFilter(type.toFuzzyType(emptyList())) else ByTypeFilter.All
            return ExpectedInfo(filter, callable.name.asString(), null, additionalData = ReturnValueAdditionalData(callable))
        }
    }
}

val ExpectedInfo.fuzzyType: FuzzyType?
    get() = filter.fuzzyType

val ExpectedInfo.multipleFuzzyTypes: Collection<FuzzyType>
    get() = filter.multipleFuzzyTypes

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

object PropertyDelegateAdditionalData : ExpectedInfo.AdditionalData

class ComparisonOperandAdditionalData(val suppressNullLiteral: Boolean) : ExpectedInfo.AdditionalData

class ExpectedInfos(
        private val bindingContext: BindingContext,
        private val resolutionFacade: ResolutionFacade,
        private val indicesHelper: KotlinIndicesHelper?,
        private val useHeuristicSignatures: Boolean = true,
        private val useOuterCallsExpectedTypeCount: Int = 0
) {
    fun calculate(expressionWithType: KtExpression): Collection<ExpectedInfo> {
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
                            ?: calculateForPropertyDelegate(expressionWithType)
                            ?: getFromBindingContext(expressionWithType)
                            ?: return emptyList()
        return expectedInfos.filterNot { it.fuzzyType?.type?.isError ?: false }
    }

    private fun calculateForArgument(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val argument = expressionWithType.parent as? KtValueArgument ?: return null
        val argumentList = argument.parent as? KtValueArgumentList ?: return null
        val callElement = argumentList.parent as? KtCallElement ?: return null
        return calculateForArgument(callElement, argument)
    }

    private fun calculateForFunctionLiteralArgument(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val functionLiteralArgument = expressionWithType.parent as? KtLambdaArgument
        val callExpression = functionLiteralArgument?.parent as? KtCallExpression ?: return null
        val literalArgument = callExpression.lambdaArguments.firstOrNull() ?: return null
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
        // sometimes we get wrong call (see testEA70945) TODO: refactor resolve so that it does not happen
        if (call.callElement != callElement) return null
        return calculateForArgument(call, argument)
    }

    fun calculateForArgument(call: Call, argument: ValueArgument): Collection<ExpectedInfo> {
        val results = calculateForArgument(call, TypeUtils.NO_EXPECTED_TYPE, argument)

        fun makesSenseToUseOuterCallExpectedType(info: ExpectedInfo): Boolean {
            val data = info.additionalData as ArgumentPositionData
            return info.fuzzyType != null
                   && info.fuzzyType!!.freeParameters.isNotEmpty()
                   && data.function.fuzzyReturnType()?.freeParameters?.isNotEmpty() ?: false
        }

        if (useOuterCallsExpectedTypeCount > 0 && results.any(::makesSenseToUseOuterCallExpectedType)) {
            val callExpression = (call.callElement as? KtExpression)?.getQualifiedExpressionForSelectorOrThis() ?: return results
            val expectedFuzzyTypes = ExpectedInfos(bindingContext, resolutionFacade, indicesHelper, useHeuristicSignatures, useOuterCallsExpectedTypeCount - 1)
                    .calculate(callExpression)
                    .mapNotNull { it.fuzzyType }
            if (expectedFuzzyTypes.isEmpty() || expectedFuzzyTypes.any { it.freeParameters.isNotEmpty() }) return results

            return expectedFuzzyTypes
                    .map { it.type }
                    .toSet()
                    .flatMap { calculateForArgument(call, it, argument) }
        }

        return results
    }

    private fun calculateForArgument(call: Call, callExpectedType: KotlinType, argument: ValueArgument): Collection<ExpectedInfo> {

        if (call is CallTransformer.CallForImplicitInvoke)
            return calculateForArgument(call.outerCall, callExpectedType, argument)

        val argumentIndex = call.valueArguments.indexOf(argument)
        assert(argumentIndex >= 0) {
            "Could not find argument '$argument(${argument.asElement().text})' among arguments of call: $call. Call element text: '${call.callElement.text}'"
        }

        // leave only arguments before the current one
        val truncatedCall = object : DelegatingCall(call) {
            val arguments = call.valueArguments.subList(0, argumentIndex)

            override fun getValueArguments() = arguments
            override fun getFunctionLiteralArguments() = emptyList<LambdaArgument>()
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

        var descriptor = candidate.resultingDescriptor
        if (descriptor.valueParameters.isEmpty()) return

        val argumentToParameter = call.mapArgumentsToParameters(descriptor)
        var parameter = argumentToParameter[argument]
        var parameterType = parameter?.type

        if (parameterType != null && parameterType.containsError()) {
            val originalParameter = descriptor.original.valueParameters[parameter!!.index]
            parameter = originalParameter
            descriptor = descriptor.original
            parameterType = fixSubstitutedType(parameterType, originalParameter.type)
        }

        val argumentName = argument.getArgumentName()?.asName
        val isFunctionLiteralArgument = argument is LambdaArgument

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
                parameterType = null
            }
            parameters = parameters.dropLast(1)
        }

        if (parameter == null) {
            if (argumentPositionData is ArgumentPositionData.Positional && argumentPositionData.namedArgumentCandidates.isNotEmpty()) {
                add(ExpectedInfo.createForNamedArgumentExpected(argumentPositionData))
            }
            return
        }
        parameterType!!

        val expectedName = if (descriptor.hasSynthesizedParameterNames()) null else parameter.name.asString()

        fun needCommaForParameter(parameter: ValueParameterDescriptor): Boolean {
            if (parameter.hasDefaultValue()) return false // parameter is optional
            if (parameter.varargElementType != null) return false // vararg arguments list can be empty
            // last parameter of functional type can be placed outside parenthesis:
            if (!isArrayAccess && parameter == parameters.last() && parameter.type.isFunctionType) return false
            return true
        }

        val tail = if (argumentName == null) {
            when {
                parameter == parameters.last() -> rparenthTail
                parameters.dropWhile { it != parameter }.drop(1).any(::needCommaForParameter) -> Tail.COMMA
                else -> null
            }
        }
        else {
            namedArgumentTail(argumentToParameter, argumentName, descriptor)
        }

        val alreadyHasStar = argument.getSpreadElement() != null

        val varargElementType = parameter.varargElementType
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
            add(ExpectedInfo.createForArgument(parameterType, expectedName, varargTail, argumentPositionData, starOptions))
        }
        else {
            if (alreadyHasStar) return

            if (isFunctionLiteralArgument) {
                if (parameterType.isFunctionType) {
                    add(ExpectedInfo.createForArgument(parameterType, expectedName, null, argumentPositionData))
                }
            }
            else {
                add(ExpectedInfo.createForArgument(parameterType, expectedName, tail, argumentPositionData))
            }
        }
    }

    private fun fixSubstitutedType(substitutedType: KotlinType, originalType: KotlinType): KotlinType {
        if (substitutedType.isError) return originalType
        if (substitutedType.arguments.size != originalType.arguments.size) return originalType
        val newTypeArguments = substitutedType.arguments.zip(originalType.arguments).map { (argument, originalArgument) ->
            if (argument.type.containsError()) originalArgument else argument
        }
        return substitutedType.replace(newTypeArguments)
    }

    private fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMatched()
            = call.valueArguments.none { argument -> getArgumentMapping(argument).isError() && !argument.hasError() /* ignore arguments that has error type */ }

    private fun ValueArgument.hasError()
            = getArgumentExpression()?.let { bindingContext.getType(it) }?.isError ?: true

    private fun namedArgumentTail(argumentToParameter: Map<ValueArgument, ValueParameterDescriptor>, argumentName: Name, descriptor: FunctionDescriptor): Tail? {
        val usedParameterNames = (argumentToParameter.values.map { it.name } + listOf(argumentName)).toSet()
        val notUsedParameters = descriptor.valueParameters.filter { it.name !in usedParameterNames }
        return when {
            notUsedParameters.isEmpty() -> Tail.RPARENTH // named arguments no supported for []
            notUsedParameters.all { it.hasDefaultValue() } -> null
            else -> Tail.COMMA
        }
    }

    private fun calculateForEqAndAssignment(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? KtBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.operationToken
            if (operationToken == KtTokens.EQ || operationToken in COMPARISON_TOKENS) {
                val otherOperand = if (expressionWithType == binaryExpression.right) binaryExpression.left else binaryExpression.right
                if (otherOperand != null) {
                    var expectedType = bindingContext.getType(otherOperand) ?: return null

                    val expectedName = expectedNameFromExpression(otherOperand)

                    if (expectedType.isNullableNothing()) { // other operand is 'null'
                        return listOf(ExpectedInfo(NullableTypesFilter, expectedName, null))
                    }

                    var additionalData: ExpectedInfo.AdditionalData? = null
                    if (operationToken in COMPARISON_TOKENS) {
                        // if we complete argument of == or !=, make types in expected info's nullable to allow items of nullable type too
                        additionalData = ComparisonOperandAdditionalData(suppressNullLiteral = expectedType.nullability() == TypeNullability.NOT_NULL)
                        expectedType = expectedType.makeNullable()
                    }

                    return listOf(ExpectedInfo(expectedType, expectedName, null, additionalData = additionalData))
                }
            }
        }
        return null
    }

    private object NullableTypesFilter : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType)
                = if (descriptorType.type.nullability() != TypeNullability.NOT_NULL) TypeSubstitutor.EMPTY else null
    }

    private fun calculateForIf(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val ifExpression = (expressionWithType.parent as? KtContainerNode)?.parent as? KtIfExpression ?: return null
        when (expressionWithType) {
            ifExpression.condition -> return listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.booleanType, null, Tail.RPARENTH, additionalData = IfConditionAdditionalData))

            ifExpression.then -> return calculate(ifExpression).map { ExpectedInfo(it.filter, it.expectedName, Tail.ELSE) }

            ifExpression.`else` -> {
                val ifExpectedInfos = calculate(ifExpression)
                val thenType = ifExpression.then?.let { bindingContext.getType(it) }

                if (ifExpectedInfos.any { it.fuzzyType != null }) {
                    val filteredInfo = if (thenType != null && !thenType.isError)
                        ifExpectedInfos.filter { it.matchingSubstitutor(thenType) != null }
                    else
                        ifExpectedInfos
                    return filteredInfo.copyWithNoAdditionalData()
                }
                else if (thenType != null) {
                    return listOf(ExpectedInfo(thenType, null, null))
                }
            }
        }

        return null
    }

    private fun calculateForElvis(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? KtBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.operationToken
            if (operationToken == KtTokens.ELVIS && expressionWithType == binaryExpression.right) {
                val leftExpression = binaryExpression.left ?: return null
                val leftType = bindingContext.getType(leftExpression)
                val leftTypeNotNullable = leftType?.makeNotNullable()
                val expectedInfos = calculate(binaryExpression)
                if (expectedInfos.any { it.fuzzyType != null }) {
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
        return if (functionLiteral != null) {
            val literalExpression = functionLiteral.parent as KtLambdaExpression
            calculate(literalExpression)
                    .mapNotNull { it.fuzzyType }
                    .filter { it.type.isFunctionType }
                    .map {
                        val returnType = it.type.getReturnTypeFromFunctionType()
                        ExpectedInfo(returnType.toFuzzyType(it.freeParameters), null, Tail.RBRACE)
                    }
        }
        else {
            calculate(block).map { ExpectedInfo(it.filter, it.expectedName, null) }
        }
    }

    private fun calculateForWhenEntryValue(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val condition = expressionWithType.parent as? KtWhenConditionWithExpression ?: return null
        val entry = condition.parent as KtWhenEntry
        val whenExpression = entry.parent as KtWhenExpression
        val subject = whenExpression.subjectExpression
        if (subject != null) {
            val subjectType = bindingContext.getType(subject) ?: return null
            return listOf(ExpectedInfo(subjectType, null, null, additionalData = WhenEntryAdditionalData(whenWithSubject = true)))
        }
        else {
            return listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.booleanType, null, null, additionalData = WhenEntryAdditionalData(whenWithSubject = false)))
        }
    }

    private fun calculateForExclOperand(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val prefixExpression = expressionWithType.parent as? KtPrefixExpression ?: return null
        if (prefixExpression.operationToken != KtTokens.EXCL) return null
        return listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.booleanType, null, null))
    }

    private fun calculateForInitializer(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val property = expressionWithType.parent as? KtProperty ?: return null
        if (expressionWithType != property.initializer) return null
        val propertyDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? VariableDescriptor ?: return null
        val expectedName = propertyDescriptor.name.asString()
        val returnTypeToUse = returnTypeToUse(propertyDescriptor, hasExplicitReturnType = property.typeReference != null)
        val expectedInfo = if (returnTypeToUse != null)
            ExpectedInfo(returnTypeToUse, expectedName, null)
        else
            ExpectedInfo(ByTypeFilter.All, expectedName, null) // no explicit type or type from base - only expected name known
        return listOf(expectedInfo)
    }

    private fun calculateForExpressionBody(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val declaration = expressionWithType.parent as? KtDeclarationWithBody ?: return null
        if (expressionWithType != declaration.bodyExpression || declaration.hasBlockBody()) return null
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? FunctionDescriptor ?: return null
        return listOfNotNull(functionReturnValueExpectedInfo(descriptor, hasExplicitReturnType = declaration.hasDeclaredReturnType()))
    }

    private fun calculateForReturn(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val returnExpression = expressionWithType.parent as? KtReturnExpression ?: return null
        val descriptor = returnExpression.getTargetFunctionDescriptor(bindingContext) ?: return null
        return listOfNotNull(functionReturnValueExpectedInfo(descriptor, hasExplicitReturnType = true))
    }

    private fun functionReturnValueExpectedInfo(descriptor: FunctionDescriptor, hasExplicitReturnType: Boolean): ExpectedInfo? {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> {
                ExpectedInfo.createForReturnValue(returnTypeToUse(descriptor, hasExplicitReturnType), descriptor)
            }

            is PropertyGetterDescriptor -> {
                val property = descriptor.correspondingProperty
                ExpectedInfo.createForReturnValue(returnTypeToUse(property, hasExplicitReturnType), property)
            }

            else -> null
        }
    }

    private fun returnTypeToUse(descriptor: CallableDescriptor, hasExplicitReturnType: Boolean): KotlinType? {
        return if (hasExplicitReturnType)
            descriptor.returnType
        else
            descriptor.overriddenDescriptors.singleOrNull()?.returnType
    }

    private fun calculateForLoopRange(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val forExpression = (expressionWithType.parent as? KtContainerNode)
                                    ?.parent as? KtForExpression ?: return null
        if (expressionWithType != forExpression.loopRange) return null

        val loopVar = forExpression.loopParameter
        val loopVarType = if (loopVar?.typeReference != null)
            (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, loopVar] as VariableDescriptor).type.takeUnless { it.isError }
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
        val detector = TypesWithContainsDetector(scope, indicesHelper, leftOperandType)

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? {
                return detector.findOperator(descriptorType)?.second
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, null))
    }

    private fun calculateForPropertyDelegate(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val delegate = expressionWithType.parent as? KtPropertyDelegate ?: return null
        val propertyDeclaration = delegate.parent as? KtProperty ?: return null
        val property = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, propertyDeclaration] as? PropertyDescriptor ?: return null

        val scope = expressionWithType.getResolutionScope(bindingContext, resolutionFacade)
        val propertyOwnerType = property.fuzzyExtensionReceiverType()
                            ?: property.dispatchReceiverParameter?.type?.toFuzzyType(emptyList())
                            ?: property.builtIns.nullableNothingType.toFuzzyType(emptyList())

        val explicitPropertyType = property.fuzzyReturnType()?.takeIf { propertyDeclaration.typeReference != null }
                                   ?: property.overriddenDescriptors.singleOrNull()?.fuzzyReturnType() // for override properties use super property type as explicit (if not specified)
        val typesWithGetDetector = TypesWithGetValueDetector(scope, indicesHelper, propertyOwnerType, explicitPropertyType)
        val typesWithSetDetector = if (property.isVar) TypesWithSetValueDetector(scope, indicesHelper, propertyOwnerType) else null

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? {
                val (getValueOperator, getOperatorSubstitutor) = typesWithGetDetector.findOperator(descriptorType) ?: return null

                if (typesWithSetDetector == null) return getOperatorSubstitutor

                val substitutedType = getOperatorSubstitutor.substitute(descriptorType.type, Variance.INVARIANT)!!.toFuzzyType(descriptorType.freeParameters)

                val (setValueOperator, setOperatorSubstitutor) = typesWithSetDetector.findOperator(substitutedType) ?: return null
                val propertyType = explicitPropertyType ?: getValueOperator.fuzzyReturnType()!!
                val setParamType = setValueOperator.valueParameters.last().type.toFuzzyType(setValueOperator.typeParameters)
                val setParamTypeSubstitutor = setParamType.checkIsSuperTypeOf(propertyType) ?: return null
                return getOperatorSubstitutor
                        .combineIfNoConflicts(setOperatorSubstitutor, descriptorType.freeParameters)
                        ?.combineIfNoConflicts(setParamTypeSubstitutor, descriptorType.freeParameters)
            }

            override val multipleFuzzyTypes: Collection<FuzzyType> by lazy {
                val result = ArrayList<FuzzyType>()

                for (classDescriptor in typesWithGetDetector.classesWithMemberOperators) {
                    val type = classDescriptor.defaultType
                    val typeParameters = classDescriptor.declaredTypeParameters
                    val substitutor = matchingSubstitutor(type.toFuzzyType(typeParameters)) ?: continue
                    result.add(substitutor.substitute(type, Variance.INVARIANT)!!.toFuzzyType(typeParameters))
                }

                for (extensionOperator in typesWithGetDetector.extensionOperators) {
                    val receiverType = extensionOperator.fuzzyExtensionReceiverType()!!
                    val substitutor = matchingSubstitutor(receiverType) ?: continue
                    result.add(substitutor.substitute(receiverType.type, Variance.INVARIANT)!!.toFuzzyType(receiverType.freeParameters))
                }

                result
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, null, additionalData = PropertyDelegateAdditionalData))
    }

    private fun getFromBindingContext(expressionWithType: KtExpression): Collection<ExpectedInfo>? {
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
        return listOf(ExpectedInfo(expectedType, null, null))
    }

    private fun expectedNameFromExpression(expression: KtExpression?): String? {
        return when (expression) {
            is KtSimpleNameExpression -> expression.getReferencedName()
            is KtQualifiedExpression -> expectedNameFromExpression(expression.selectorExpression)
            is KtCallExpression -> expectedNameFromExpression(expression.calleeExpression)
            is KtArrayAccessExpression -> expectedNameFromExpression(expression.arrayExpression)?.unpluralize()
            else -> null
        }
    }

    private fun String.unpluralize()
            = StringUtil.unpluralize(this)

    private fun Collection<ExpectedInfo>.copyWithNoAdditionalData() = map { it.copy(additionalData = null, itemOptions = ItemOptions.DEFAULT) }
}

val COMPARISON_TOKENS = setOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)

