/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.replacement

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.analysis.computeTypeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findLocalVariable
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

object ReplacementEngine {
    fun <TCallElement : KtElement> performCallReplacement(
            element: KtSimpleNameExpression,
            bindingContext: BindingContext,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            callElement: TCallElement,
            callKindHandler: CallKindHandler<TCallElement>,
            replacement: ReplacementExpression
    ): KtElement {
        val project = element.project
        val psiFactory = KtPsiFactory(project)
        val descriptor = resolvedCall.resultingDescriptor

        val qualifiedExpression = callElement.getQualifiedExpressionForSelector()
        val elementToBeReplaced = callKindHandler.elementToReplace(callElement)

        val commentSaver = CommentSaver(elementToBeReplaced, saveLineBreaks = true)

        var receiver = element.getReceiverExpression()?.marked(USER_CODE_KEY)
        var receiverType = if (receiver != null) bindingContext.getType(receiver) else null

        if (receiver == null) {
            val receiverValue = if (descriptor.isExtension) resolvedCall.extensionReceiver else resolvedCall.dispatchReceiver
            val resolutionScope = elementToBeReplaced.getResolutionScope(bindingContext, elementToBeReplaced.getResolutionFacade())
            if (receiverValue is ImplicitReceiver) {
                receiver = receiverValue.asExpression(resolutionScope, psiFactory)
                receiverType = receiverValue.type
            }
        }

        receiver?.mark(RECEIVER_VALUE_KEY)

        val wrapper = if (elementToBeReplaced is KtExpression)
            ConstructedExpressionWrapperWithIntroduceFeature(replacement.expression, bindingContext, elementToBeReplaced)
        else
            ConstructedExpressionWrapper(replacement.expression, bindingContext)

        //TODO: this@
        for (thisExpression in replacement.expression.collectDescendantsOfType<KtThisExpression>()) {
            if (receiver != null) {
                wrapper.replaceExpression(thisExpression, receiver)
            }
            else {
                thisExpression.mark(RECEIVER_VALUE_KEY)
            }
        }

        val introduceValuesForParameters = wrapper.processValueParameterUsages(resolvedCall, project)

        wrapper.processTypeParameterUsages(resolvedCall)

        if (qualifiedExpression is KtSafeQualifiedExpression) {
            (wrapper as ConstructedExpressionWrapperWithIntroduceFeature).wrapExpressionForSafeCall(receiver!!, receiverType)
        }
        else if (callElement is KtBinaryExpression && callElement.operationToken == KtTokens.IDENTIFIER) {
            wrapper.keepInfixFormIfPossible()
        }

        if (receiver != null && wrapper is ConstructedExpressionWrapperWithIntroduceFeature) {
            val thisReplaced = wrapper.expression.collectDescendantsOfType<KtExpression> { it[RECEIVER_VALUE_KEY] }
            if (receiver.shouldKeepValue(thisReplaced.size)) {
                wrapper.introduceValue(receiver, receiverType, thisReplaced)
            }
        }

        if (wrapper is ConstructedExpressionWrapperWithIntroduceFeature) {
            for ((parameter, value, valueType) in introduceValuesForParameters) {
                val usagesReplaced = wrapper.expression.collectDescendantsOfType<KtExpression> { it[PARAMETER_VALUE_KEY] == parameter }
                wrapper.introduceValue(value, valueType, usagesReplaced, nameSuggestion = parameter.name.asString())
            }
        }

        val wrappedExpression = callKindHandler.wrapGeneratedExpression(wrapper.expression)
        val result = elementToBeReplaced.replace(wrappedExpression) as KtElement

        val file = result.getContainingKtFile()
        replacement.fqNamesToImport
                .flatMap { file.resolveImportReference(it) }
                .forEach { ImportInsertHelper.getInstance(project).importDescriptor(file, it) }

        var resultRange = if (wrapper !is ConstructedExpressionWrapperWithIntroduceFeature || wrapper.addedStatements.isEmpty())
            PsiChildRange.singleElement(result)
        else
            PsiChildRange(wrapper.addedStatements.first(), result)

        resultRange = postProcessInsertedCode(resultRange)

        commentSaver.restore(resultRange)

        @Suppress("UNCHECKED_CAST")
        return callKindHandler.unwrapResult(resultRange.last as TCallElement)
    }

    private fun ConstructedExpressionWrapper.processValueParameterUsages(
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            project: Project
    ): Collection<IntroduceValueForParameter> {
        val introduceValuesForParameters = ArrayList<IntroduceValueForParameter>()

        // process parameters in reverse order because default values can use previous parameters
        val parameters = resolvedCall.resultingDescriptor.valueParameters
        for (parameter in parameters.asReversed()) {
            val argument = argumentForParameter(parameter, resolvedCall, bindingContext, project) ?: continue

            argument.expression.put(PARAMETER_VALUE_KEY, parameter)

            val parameterName = parameter.name
            val usages = expression.collectDescendantsOfType<KtExpression> {
                it[ReplacementExpression.PARAMETER_USAGE_KEY] == parameterName
            }
            usages.forEach {
                val usageArgument = it.parent as? KtValueArgument
                if (argument.isNamed) {
                    usageArgument?.mark(MAKE_ARGUMENT_NAMED_KEY)
                }
                if (argument.isDefaultValue) {
                    usageArgument?.mark(DEFAULT_PARAMETER_VALUE_KEY)
                }
                replaceExpression(it, argument.expression)
            }

            //TODO: sometimes we need to add explicit type arguments here because we don't have expected type in the new context

            if (argument.expression.shouldKeepValue(usages.size)) {
                introduceValuesForParameters.add(IntroduceValueForParameter(parameter, argument.expression, argument.expressionType))
            }
        }

        return introduceValuesForParameters
    }

    private data class IntroduceValueForParameter(
            val parameter: ValueParameterDescriptor,
            val value: KtExpression,
            val valueType: KotlinType?)

    private fun ConstructedExpressionWrapper.processTypeParameterUsages(resolvedCall: ResolvedCall<out CallableDescriptor>) {
        val typeParameters = resolvedCall.resultingDescriptor.original.typeParameters

        val callElement = resolvedCall.call.callElement
        val callExpression = callElement as? KtCallExpression
        val explicitTypeArgs = callExpression?.typeArgumentList?.arguments
        if (explicitTypeArgs != null && explicitTypeArgs.size != typeParameters.size) return

        for ((index, typeParameter) in typeParameters.withIndex()) {
            val parameterName = typeParameter.name
            val usages = expression.collectDescendantsOfType<KtExpression> {
                it[ReplacementExpression.TYPE_PARAMETER_USAGE_KEY] == parameterName
            }

            val factory = KtPsiFactory(callElement)
            val type = resolvedCall.typeArguments[typeParameter]!!
            val typeElement = if (explicitTypeArgs != null) { // we use explicit type arguments if available to avoid shortening
                val _typeElement = explicitTypeArgs[index].typeReference?.typeElement ?: continue
                _typeElement.marked(USER_CODE_KEY)
            }
            else {
                factory.createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)).typeElement!!
            }

            val typeClassifier = type.constructor.declarationDescriptor

            for (usage in usages) {
                val parent = usage.parent
                if (parent is KtClassLiteralExpression && typeClassifier != null) {
                    // for class literal ("X::class") we need type arguments only for kotlin.Array
                    val arguments =
                            if (typeElement is KtUserType && KotlinBuiltIns.isArray(type)) typeElement.typeArgumentList?.text.orEmpty()
                            else ""
                    replaceExpression(usage, KtPsiFactory(usage).createExpression(
                            IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(typeClassifier) + arguments
                    ))
                }
                else if (parent is KtUserType) {
                    parent.replace(typeElement)
                }
                else {
                    //TODO: tests for this?
                    replaceExpression(usage, KtPsiFactory(usage).createExpression(typeElement.text))
                }
            }
        }
    }

    private fun ConstructedExpressionWrapperWithIntroduceFeature.wrapExpressionForSafeCall(receiver: KtExpression, receiverType: KotlinType?) {
        val qualified = expression as? KtQualifiedExpression
        if (qualified != null) {
            if (qualified.receiverExpression[RECEIVER_VALUE_KEY]) {
                if (qualified is KtSafeQualifiedExpression) return // already safe
                val selector = qualified.selectorExpression
                if (selector != null) {
                    expression = psiFactory.createExpressionByPattern("$0?.$1", receiver, selector)
                    return
                }
            }
        }

        if (expressionToBeReplaced.isUsedAsExpression(bindingContext)) {
            val thisReplaced = expression.collectDescendantsOfType<KtExpression> { it[RECEIVER_VALUE_KEY] }
            introduceValue(receiver, receiverType, thisReplaced, safeCall = true)
        }
        else {
            expression = psiFactory.createExpressionByPattern("if ($0 != null) { $1 }", receiver, expression)
        }
    }

    private fun ConstructedExpressionWrapper.keepInfixFormIfPossible() {
        val dotQualified = expression as? KtDotQualifiedExpression ?: return
        val receiver = dotQualified.receiverExpression
        if (!receiver[RECEIVER_VALUE_KEY]) return
        val call = dotQualified.selectorExpression as? KtCallExpression ?: return
        val nameExpression = call.calleeExpression as? KtSimpleNameExpression ?: return
        val argument = call.valueArguments.singleOrNull() ?: return
        if (argument.getArgumentName() != null) return
        val argumentExpression = argument.getArgumentExpression() ?: return
        expression = psiFactory.createExpressionByPattern("$0 ${nameExpression.text} $1", receiver, argumentExpression)
    }

    private fun KtExpression?.shouldKeepValue(usageCount: Int): Boolean {
        if (usageCount == 1) return false
        val sideEffectOnly = usageCount == 0

        return when (this) {
            is KtSimpleNameExpression -> false
            is KtQualifiedExpression -> receiverExpression.shouldKeepValue(usageCount) || selectorExpression.shouldKeepValue(usageCount)
            is KtUnaryExpression -> operationToken in setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) || baseExpression.shouldKeepValue(usageCount)
            is KtStringTemplateExpression -> entries.any { if (sideEffectOnly) it.expression.shouldKeepValue(usageCount) else it is KtStringTemplateEntryWithExpression }
            is KtThisExpression, is KtSuperExpression, is KtConstantExpression -> false
            is KtParenthesizedExpression -> expression.shouldKeepValue(usageCount)
            is KtArrayAccessExpression -> if (sideEffectOnly) arrayExpression.shouldKeepValue(usageCount) || indexExpressions.any { it.shouldKeepValue(usageCount) } else true
            is KtBinaryExpression -> if (sideEffectOnly) left.shouldKeepValue(usageCount) || right.shouldKeepValue(usageCount) else true
            is KtIfExpression -> if (sideEffectOnly) condition.shouldKeepValue(usageCount) || then.shouldKeepValue(usageCount) || `else`.shouldKeepValue(usageCount) else true
            is KtBinaryExpressionWithTypeRHS -> true
            else -> true
        }
    }

    private class Argument(
            val expression: KtExpression,
            val expressionType: KotlinType?,
            val isNamed: Boolean = false,
            val isDefaultValue: Boolean = false)

    private fun argumentForParameter(
            parameter: ValueParameterDescriptor,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            bindingContext: BindingContext,
            project: Project): Argument? {
        val resolvedArgument = resolvedCall.valueArguments[parameter]!!
        when (resolvedArgument) {
            is ExpressionValueArgument -> {
                val valueArgument = resolvedArgument.valueArgument!!
                val expression = valueArgument.getArgumentExpression()!!
                expression.mark(USER_CODE_KEY)
                if (valueArgument is LambdaArgument) {
                    expression.mark(WAS_FUNCTION_LITERAL_ARGUMENT_KEY)
                }
                return Argument(expression, bindingContext.getType(expression), isNamed = valueArgument.isNamed())
            }

            is DefaultValueArgument -> {
                val defaultValue = OptionalParametersHelper.defaultParameterValue(parameter, project) ?: return null
                val (expression, parameterUsages) = defaultValue

                for ((param, usages) in parameterUsages) {
                    usages.forEach { it.put(ReplacementExpression.PARAMETER_USAGE_KEY, param.name) }
                }

                val expressionCopy = expression.copied()

                // clean up user data in original
                expression.forEachDescendantOfType<KtExpression> { it.clear(ReplacementExpression.PARAMETER_USAGE_KEY) }

                return Argument(expressionCopy, null/*TODO*/, isDefaultValue = true)
            }

            is VarargValueArgument -> {
                val arguments = resolvedArgument.arguments
                val single = arguments.singleOrNull()
                if (single != null && single.getSpreadElement() != null) {
                    val expression = single.getArgumentExpression()!!.marked(USER_CODE_KEY)
                    return Argument(expression, bindingContext.getType(expression), isNamed = single.isNamed())
                }

                val elementType = parameter.varargElementType!!
                val expression = KtPsiFactory(project).buildExpression {
                    appendFixedText(arrayOfFunctionName(elementType))
                    appendFixedText("(")
                    for ((i, argument) in arguments.withIndex()) {
                        if (i > 0) appendFixedText(",")
                        if (argument.getSpreadElement() != null) {
                            appendFixedText("*")
                        }
                        appendExpression(argument.getArgumentExpression()!!.marked(USER_CODE_KEY))
                    }
                    appendFixedText(")")
                }
                return Argument(expression, parameter.type, isNamed = single?.isNamed() ?: false)
            }

            else -> error("Unknown argument type: $resolvedArgument")
        }
    }

    private fun postProcessInsertedCode(range: PsiChildRange): PsiChildRange {
        val elements = range.filterIsInstance<KtElement>().toList()

        elements.forEach {
            introduceNamedArguments(it)

            restoreFunctionLiteralArguments(it)

            //TODO: do this earlier
            dropArgumentsForDefaultValues(it)

            simplifySpreadArrayOfArguments(it)

            removeExplicitTypeArguments(it)
        }


        val shortenFilter = { element: PsiElement ->
            if (element[USER_CODE_KEY]) {
                ShortenReferences.FilterResult.SKIP
            }
            else {
                val thisReceiver = (element as? KtQualifiedExpression)?.receiverExpression as? KtThisExpression
                if (thisReceiver != null && thisReceiver[USER_CODE_KEY]) // don't remove explicit 'this' coming from user's code
                    ShortenReferences.FilterResult.GO_INSIDE
                else
                    ShortenReferences.FilterResult.PROCESS
            }
        }

        val newElements = elements.map {
            ShortenReferences({ ShortenReferences.Options(removeThis = true) }).process(it, shortenFilter)
        }

        newElements.forEach {
            // clean up user data
            it.forEachDescendantOfType<KtExpression> {
                it.clear(USER_CODE_KEY)
                it.clear(ReplacementExpression.PARAMETER_USAGE_KEY)
                it.clear(ReplacementExpression.TYPE_PARAMETER_USAGE_KEY)
                it.clear(PARAMETER_VALUE_KEY)
                it.clear(RECEIVER_VALUE_KEY)
                it.clear(WAS_FUNCTION_LITERAL_ARGUMENT_KEY)
            }
            it.forEachDescendantOfType<KtValueArgument> {
                it.clear(MAKE_ARGUMENT_NAMED_KEY)
                it.clear(DEFAULT_PARAMETER_VALUE_KEY)
            }
        }

        return PsiChildRange(newElements.first(), newElements.last())
    }

    private fun introduceNamedArguments(result: KtElement) {
        val callsToProcess = LinkedHashSet<KtCallExpression>()
        result.forEachDescendantOfType<KtValueArgument> {
            if (it[MAKE_ARGUMENT_NAMED_KEY] && !it.isNamed()) {
                val callExpression = (it.parent as? KtValueArgumentList)?.parent as? KtCallExpression
                callsToProcess.addIfNotNull(callExpression)
            }
        }

        val psiFactory = KtPsiFactory(result)

        for (callExpression in callsToProcess) {
            val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return
            if (!resolvedCall.isReallySuccess()) return

            val argumentsToMakeNamed = callExpression.valueArguments.dropWhile { !it[MAKE_ARGUMENT_NAMED_KEY] }
            for (argument in argumentsToMakeNamed) {
                if (argument.isNamed()) continue
                if (argument is KtLambdaArgument) continue
                val argumentMatch = resolvedCall.getArgumentMapping(argument) as ArgumentMatch
                val name = argumentMatch.valueParameter.name
                //TODO: not always correct for vararg's
                val newArgument = psiFactory.createArgument(argument.getArgumentExpression()!!, name, argument.getSpreadElement() != null)

                if (argument[DEFAULT_PARAMETER_VALUE_KEY]) {
                    newArgument.mark(DEFAULT_PARAMETER_VALUE_KEY)
                }

                argument.replace(newArgument)
            }
        }
    }

    private fun dropArgumentsForDefaultValues(result: KtElement) {
        val project = result.project
        val newBindingContext = result.analyze()
        val argumentsToDrop = ArrayList<ValueArgument>()

        // we drop only those arguments that added to the code from some parameter's default
        fun canDropArgument(argument: ValueArgument) = (argument as KtValueArgument)[DEFAULT_PARAMETER_VALUE_KEY]

        result.forEachDescendantOfType<KtCallExpression> { callExpression ->
            val resolvedCall = callExpression.getResolvedCall(newBindingContext) ?: return@forEachDescendantOfType

            argumentsToDrop.addAll(OptionalParametersHelper.detectArgumentsToDropForDefaults(resolvedCall, project, ::canDropArgument))
        }

        for (argument in argumentsToDrop) {
            argument as KtValueArgument
            val argumentList = argument.parent as KtValueArgumentList
            argumentList.removeArgument(argument)
            if (argumentList.arguments.isEmpty()) {
                val callExpression = argumentList.parent as KtCallExpression
                if (callExpression.lambdaArguments.isNotEmpty()) {
                    argumentList.delete()
                }
            }
        }
    }

    private fun arrayOfFunctionName(elementType: KotlinType): String {
        return when {
            KotlinBuiltIns.isInt(elementType) -> "kotlin.intArrayOf"
            KotlinBuiltIns.isLong(elementType) -> "kotlin.longArrayOf"
            KotlinBuiltIns.isShort(elementType) -> "kotlin.shortArrayOf"
            KotlinBuiltIns.isChar(elementType) -> "kotlin.charArrayOf"
            KotlinBuiltIns.isBoolean(elementType) -> "kotlin.booleanArrayOf"
            KotlinBuiltIns.isByte(elementType) -> "kotlin.byteArrayOf"
            KotlinBuiltIns.isDouble(elementType) -> "kotlin.doubleArrayOf"
            KotlinBuiltIns.isFloat(elementType) -> "kotlin.floatArrayOf"
            elementType.isError -> "kotlin.arrayOf"
            else -> "kotlin.arrayOf<" + IdeDescriptorRenderers.SOURCE_CODE.renderType(elementType) + ">"
        }
    }

    private fun removeExplicitTypeArguments(result: KtElement) {
        result.collectDescendantsOfType<KtTypeArgumentList>(canGoInside = { !it[USER_CODE_KEY] }) {
            RemoveExplicitTypeArgumentsIntention.isApplicableTo(it, approximateFlexible = true)
        }.forEach { it.delete() }
    }

    private fun simplifySpreadArrayOfArguments(result: KtElement) {
        //TODO: test for nested

        val argumentsToExpand = ArrayList<Pair<KtValueArgument, Collection<KtValueArgument>>>()

        result.forEachDescendantOfType<KtValueArgument>(canGoInside = { !it[USER_CODE_KEY] }) { argument ->
            if (argument.getSpreadElement() != null && !argument.isNamed()) {
                val argumentExpression = argument.getArgumentExpression() ?: return@forEachDescendantOfType
                val resolvedCall = argumentExpression.getResolvedCall(argumentExpression.analyze(BodyResolveMode.PARTIAL)) ?: return@forEachDescendantOfType
                val callExpression = resolvedCall.call.callElement as? KtCallExpression ?: return@forEachDescendantOfType
                if (CompileTimeConstantUtils.isArrayMethodCall(resolvedCall)) {
                    argumentsToExpand.add(argument to callExpression.valueArguments)
                }
            }
        }

        for ((argument, replacements) in argumentsToExpand) {
            argument.replaceByMultiple(replacements)
        }
    }

    private fun KtValueArgument.replaceByMultiple(arguments: Collection<KtValueArgument>) {
        val list = parent as KtValueArgumentList
        if (arguments.isEmpty()) {
            list.removeArgument(this)
        }
        else {
            var anchor = this
            for (argument in arguments) {
                anchor = list.addArgumentAfter(argument, anchor)
            }
            list.removeArgument(this)
        }
    }

    private fun restoreFunctionLiteralArguments(expression: KtElement) {
        val callExpressions = ArrayList<KtCallExpression>()

        expression.forEachDescendantOfType<KtExpression>(fun (expr) {
            if (!expr[WAS_FUNCTION_LITERAL_ARGUMENT_KEY]) return
            assert(expr.unpackFunctionLiteral() != null)

            val argument = expr.parent as? KtValueArgument ?: return
            if (argument is KtLambdaArgument) return
            if (argument.isNamed()) return
            val argumentList = argument.parent as? KtValueArgumentList ?: return
            if (argument != argumentList.arguments.last()) return
            val callExpression = argumentList.parent as? KtCallExpression ?: return
            if (callExpression.lambdaArguments.isNotEmpty()) return

            val resolvedCall = callExpression.getResolvedCall(callExpression.analyze(BodyResolveMode.PARTIAL)) ?: return
            if (!resolvedCall.isReallySuccess()) return
            val argumentMatch = resolvedCall.getArgumentMapping(argument) as ArgumentMatch
            if (argumentMatch.valueParameter != resolvedCall.resultingDescriptor.valueParameters.last()) return

            callExpressions.add(callExpression)
        })

        callExpressions.forEach { it.moveFunctionLiteralOutsideParentheses() }
    }

    internal operator fun <T: Any> PsiElement.get(key: Key<T>): T? = getCopyableUserData(key)
    internal operator fun PsiElement.get(key: Key<Unit>): Boolean = getCopyableUserData(key) != null
    private fun <T: Any> KtElement.clear(key: Key<T>) = putCopyableUserData(key, null)
    private fun <T: Any> KtElement.put(key: Key<T>, value: T) = putCopyableUserData(key, value)
    private fun KtElement.mark(key: Key<Unit>) = putCopyableUserData(key, Unit)

    private fun <T: KtElement> T.marked(key: Key<Unit>): T {
        putCopyableUserData(key, Unit)
        return this
    }

    // keys below are used on expressions
    private val USER_CODE_KEY = Key<Unit>("USER_CODE")
    private val PARAMETER_VALUE_KEY = Key<ValueParameterDescriptor>("PARAMETER_VALUE")
    private val RECEIVER_VALUE_KEY = Key<Unit>("RECEIVER_VALUE")
    private val WAS_FUNCTION_LITERAL_ARGUMENT_KEY = Key<Unit>("WAS_FUNCTION_LITERAL_ARGUMENT")

    // these keys are used on JetValueArgument
    private val MAKE_ARGUMENT_NAMED_KEY = Key<Unit>("MAKE_ARGUMENT_NAMED")
    private val DEFAULT_PARAMETER_VALUE_KEY = Key<Unit>("DEFAULT_PARAMETER_VALUE")

    private open class ConstructedExpressionWrapper(
            var expression: KtExpression,
            val bindingContext: BindingContext
    ) {
        val psiFactory = KtPsiFactory(expression)

        fun replaceExpression(oldExpression: KtExpression, newExpression: KtExpression): KtExpression {
            assert(expression.isAncestor(oldExpression))
            val result = oldExpression.replace(newExpression) as KtExpression
            if (oldExpression == expression) {
                expression = result
            }
            return result
        }
    }

    private class ConstructedExpressionWrapperWithIntroduceFeature(
            expression: KtExpression,
            bindingContext: BindingContext,
            val expressionToBeReplaced: KtExpression
    ) : ConstructedExpressionWrapper(expression, bindingContext) {
        val addedStatements = ArrayList<KtExpression>()

        fun introduceValue(
                value: KtExpression,
                valueType: KotlinType?,
                usages: Collection<KtExpression>,
                nameSuggestion: String? = null,
                safeCall: Boolean = false
        ) {
            assert(usages.all { expression.isAncestor(it, strict = true) })

            fun replaceUsages(name: Name) {
                val nameInCode = psiFactory.createExpression(name.render())
                for (usage in usages) {
                    usage.replace(nameInCode)
                }
            }

            fun suggestName(validator: (String) -> Boolean): Name {
                val name = if (nameSuggestion != null)
                    KotlinNameSuggester.suggestNameByName(nameSuggestion, validator)
                else
                    KotlinNameSuggester.suggestNamesByExpressionOnly(value, bindingContext, validator, "t").first()
                return Name.identifier(name)
            }

            // checks that name is used (without receiver) inside expression being constructed but not inside usages that will be replaced
            fun isNameUsed(name: String) = collectNameUsages(expression, name).any { nameUsage -> usages.none { it.isAncestor(nameUsage) } }

            if (!safeCall) {
                val block = expressionToBeReplaced.parent as? KtBlockExpression
                if (block != null) {
                    val resolutionScope = expressionToBeReplaced.getResolutionScope(bindingContext, expressionToBeReplaced.getResolutionFacade())

                    if (usages.isNotEmpty()) {
                        var explicitType: KotlinType? = null
                        if (valueType != null && !ErrorUtils.containsErrorType(valueType)) {
                            val valueTypeWithoutExpectedType = value.computeTypeInContext(
                                    resolutionScope,
                                    expressionToBeReplaced,
                                    dataFlowInfo = bindingContext.getDataFlowInfo(expressionToBeReplaced)
                            )
                            if (valueTypeWithoutExpectedType == null || ErrorUtils.containsErrorType(valueTypeWithoutExpectedType)) {
                                explicitType = valueType
                            }
                        }

                        val name = suggestName { name ->
                            resolutionScope.findLocalVariable(Name.identifier(name)) == null && !isNameUsed(name)
                        }

                        var declaration = psiFactory.createDeclarationByPattern<KtVariableDeclaration>("val $0 = $1", name, value)
                        declaration = block.addBefore(declaration, expressionToBeReplaced) as KtVariableDeclaration
                        block.addBefore(psiFactory.createNewLine(), expressionToBeReplaced)

                        if (explicitType != null) {
                            declaration.setType(explicitType)
                        }

                        replaceUsages(name)

                        addedStatements.add(declaration)
                    }
                    else {
                        addedStatements.add(block.addBefore(value, expressionToBeReplaced) as KtExpression)
                        block.addBefore(psiFactory.createNewLine(), expressionToBeReplaced)
                    }
                    return
                }
            }

            val dot = if (safeCall) "?." else "."

            expression = if (!isNameUsed("it")) {
                replaceUsages(Name.identifier("it"))
                psiFactory.createExpressionByPattern("$0${dot}let { $1 }", value, expression)
            }
            else {
                val name = suggestName { !isNameUsed(it) }
                replaceUsages(name)
                psiFactory.createExpressionByPattern("$0${dot}let { $1 -> $2 }", value, name, expression)
            }
        }

        private fun collectNameUsages(scope: KtExpression, name: String)
                = scope.collectDescendantsOfType<KtSimpleNameExpression> { it.getReceiverExpression() == null && it.getReferencedName() == name }
    }
}