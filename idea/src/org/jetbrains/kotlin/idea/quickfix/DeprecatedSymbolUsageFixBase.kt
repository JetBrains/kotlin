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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.ArrayList
import java.util.LinkedHashSet

//TODO: replacement of class usages
//TODO: different replacements for property accessors

public abstract class DeprecatedSymbolUsageFixBase(
        element: JetSimpleNameExpression/*TODO?*/,
        val replaceWith: ReplaceWith
) : JetIntentionAction<JetSimpleNameExpression>(element) {

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false

        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return false
        if (!resolvedCall.getStatus().isSuccess()) return false
        val descriptor = resolvedCall.getResultingDescriptor()
        if (replaceWithPattern(descriptor, project) != replaceWith) return false

        try {
            JetPsiFactory(project).createExpression(replaceWith.expression)
            return true
        }
        catch(e: Exception) {
            return false
        }
    }

    final override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val bindingContext = element.analyze()
        val resolvedCall = element.getResolvedCall(bindingContext)!!
        val descriptor = resolvedCall.getResultingDescriptor()

        val replacement = ReplaceWithAnnotationAnalyzer.analyze(replaceWith, descriptor, element.getResolutionFacade(), project)

        invoke(resolvedCall, bindingContext, replacement, project, editor)
    }

    protected abstract fun invoke(
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            bindingContext: BindingContext,
            replacement: ReplaceWithAnnotationAnalyzer.ReplacementExpression,
            project: Project,
            editor: Editor?)

    companion object {
        public fun replaceWithPattern(descriptor: DeclarationDescriptor, project: Project): ReplaceWith? {
            val annotationClass = descriptor.builtIns.getDeprecatedAnnotation()
            val annotation = descriptor.getAnnotations().findAnnotation(DescriptorUtils.getFqNameSafe(annotationClass)) ?: return null
            val replaceWithValue = annotation.argumentValue("replaceWith"/*TODO: kotlin.deprecated::replaceWith.name*/) as? AnnotationDescriptor ?: return null
            val pattern = replaceWithValue.argumentValue("expression"/*TODO: kotlin.ReplaceWith::expression.name*/) as? String ?: return null
            if (pattern.isEmpty()) return null
            val importValues = replaceWithValue.argumentValue("imports"/*TODO: kotlin.ReplaceWith::imports.name*/) as? List<*> ?: return null
            if (importValues.any { it !is StringValue }) return null
            val imports = importValues.map { (it as StringValue).getValue()!! }

            // should not be available for descriptors with optional parameters if we cannot fetch default values for them (currently for library with no sources)
            if (descriptor is CallableDescriptor &&
                descriptor.getValueParameters().any { it.hasDefaultValue() && OptionalParametersHelper.defaultParameterValue(it, project) == null }) return null

            return ReplaceWith(pattern, *imports.toTypedArray())
        }

        public fun performReplacement(
                element: JetSimpleNameExpression,
                bindingContext: BindingContext,
                resolvedCall: ResolvedCall<out CallableDescriptor>,
                replacement: ReplaceWithAnnotationAnalyzer.ReplacementExpression
        ): JetExpression {
            val project = element.getProject()
            val psiFactory = JetPsiFactory(project)
            val descriptor = resolvedCall.getResultingDescriptor()

            val callExpression = resolvedCall.getCall().getCallElement() as JetExpression
            val parent = callExpression.getParent()
            val qualifiedExpression = if (parent is JetQualifiedExpression && callExpression == parent.getSelectorExpression()) parent else null
            val expressionToBeReplaced = qualifiedExpression ?: callExpression

            val commentSaver = CommentSaver(expressionToBeReplaced, saveLineBreaks = true)

            var receiver = element.getReceiverExpression()?.marked(USER_CODE_KEY)
            var receiverType = if (receiver != null) bindingContext.getType(receiver) else null

            if (receiver == null) {
                val receiverValue = if (descriptor.isExtension) resolvedCall.getExtensionReceiver() else resolvedCall.getDispatchReceiver()
                val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expressionToBeReplaced]
                if (receiverValue is ThisReceiver && resolutionScope != null) {
                    receiver = receiverValue.asExpression(resolutionScope, psiFactory)
                    receiverType = receiverValue.getType()
                }
            }

            receiver?.mark(RECEIVER_VALUE_KEY)

            val wrapper = ConstructedExpressionWrapper(replacement.expression, expressionToBeReplaced, bindingContext)

            //TODO: this@
            for (thisExpression in replacement.expression.collectDescendantsOfType<JetThisExpression>()) {
                if (receiver != null) {
                    wrapper.replaceExpression(thisExpression, receiver)
                }
                else {
                    thisExpression.mark(RECEIVER_VALUE_KEY)
                }
            }

            val introduceValuesForParameters = wrapper.processValueParameterUsages(resolvedCall, project)

            wrapper.processTypeParameterUsages(resolvedCall)

            if (qualifiedExpression is JetSafeQualifiedExpression) {
                wrapper.wrapExpressionForSafeCall(receiver!!, receiverType)
            }
            else if (callExpression is JetBinaryExpression && callExpression.getOperationToken() == JetTokens.IDENTIFIER) {
                wrapper.keepInfixFormIfPossible()
            }

            if (receiver != null) {
                val thisReplaced = wrapper.expression.collectDescendantsOfType<JetExpression> { it[RECEIVER_VALUE_KEY] }
                if (receiver.shouldKeepValue(thisReplaced.size())) {
                    wrapper.introduceValue(receiver, receiverType, thisReplaced)
                }
            }

            for ((parameter, value, valueType) in introduceValuesForParameters) {
                val usagesReplaced = wrapper.expression.collectDescendantsOfType<JetExpression> { it[PARAMETER_VALUE_KEY] == parameter }
                wrapper.introduceValue(value, valueType, usagesReplaced, nameSuggestion = parameter.getName().asString())
            }

            var result = expressionToBeReplaced.replace(wrapper.expression) as JetExpression

            //TODO: drop import of old function (if not needed anymore)?

            val file = result.getContainingJetFile()
            replacement.fqNamesToImport
                    .flatMap { file.resolveImportReference(it) }
                    .forEach { ImportInsertHelper.getInstance(project).importDescriptor(file, it) }

            var resultRange = if (wrapper.addedStatements.isEmpty())
                PsiChildRange.singleElement(result)
            else
                PsiChildRange(wrapper.addedStatements.first(), result)

            resultRange = postProcessInsertedExpression(resultRange)

            commentSaver.restore(resultRange)

            return resultRange.last as JetExpression
        }

        private fun ConstructedExpressionWrapper.processValueParameterUsages(
                resolvedCall: ResolvedCall<out CallableDescriptor>,
                project: Project
        ): Collection<IntroduceValueForParameter> {
            val introduceValuesForParameters = ArrayList<IntroduceValueForParameter>()

            // process parameters in reverse order because default values can use previous parameters
            val parameters = resolvedCall.getResultingDescriptor().getValueParameters()
            for (parameter in parameters.reverse()) {
                val argument = argumentForParameter(parameter, resolvedCall, bindingContext, project) ?: continue

                argument.expression.put(PARAMETER_VALUE_KEY, parameter)

                val parameterName = parameter.getName()
                val usages = expression.collectDescendantsOfType<JetExpression> {
                    it[ReplaceWithAnnotationAnalyzer.PARAMETER_USAGE_KEY] == parameterName
                }
                usages.forEach {
                    val usageArgument = it.getParent() as? JetValueArgument
                    if (argument.isNamed) {
                        usageArgument?.mark(MAKE_ARGUMENT_NAMED_KEY)
                    }
                    if (argument.isDefaultValue) {
                        usageArgument?.mark(DEFAULT_PARAMETER_VALUE_KEY)
                    }
                    replaceExpression(it, argument.expression)
                }

                //TODO: sometimes we need to add explicit type arguments here because we don't have expected type in the new context

                if (argument.expression.shouldKeepValue(usages.size())) {
                    introduceValuesForParameters.add(IntroduceValueForParameter(parameter, argument.expression, argument.expressionType))
                }
            }

            return introduceValuesForParameters
        }

        private data class IntroduceValueForParameter(
                val parameter: ValueParameterDescriptor,
                val value: JetExpression,
                val valueType: JetType?)

        private fun ConstructedExpressionWrapper.processTypeParameterUsages(resolvedCall: ResolvedCall<out CallableDescriptor>) {
            val typeParameters = resolvedCall.getResultingDescriptor().getOriginal().getTypeParameters()

            val callElement = resolvedCall.getCall().getCallElement()
            val callExpression = callElement as? JetCallExpression
            val explicitTypeArgs = callExpression?.getTypeArgumentList()?.getArguments()
            if (explicitTypeArgs != null && explicitTypeArgs.size() != typeParameters.size()) return

            for ((index, typeParameter) in typeParameters.withIndex()) {
                val parameterName = typeParameter.getName()
                val usages = expression.collectDescendantsOfType<JetExpression> {
                    it[ReplaceWithAnnotationAnalyzer.TYPE_PARAMETER_USAGE_KEY] == parameterName
                }

                val factory = JetPsiFactory(callElement)
                val typeElement = if (explicitTypeArgs != null) { // we use explicit type arguments if available to avoid shortening
                    val _typeElement = explicitTypeArgs[index].getTypeReference()?.getTypeElement() ?: continue
                    _typeElement.marked(USER_CODE_KEY)
                }
                else {
                    val type = resolvedCall.getTypeArguments()[typeParameter]!!
                    factory.createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)).getTypeElement()!!
                }

                for (usage in usages) {
                    val parent = usage.getParent()
                    if (parent is JetUserType) {
                        parent.replace(typeElement)
                    }
                    else {
                        //TODO: tests for this?
                        replaceExpression(usage, JetPsiFactory(usage).createExpression(typeElement.getText()))
                    }
                }
            }
        }

        private fun ConstructedExpressionWrapper.wrapExpressionForSafeCall(receiver: JetExpression, receiverType: JetType?) {
            val qualified = expression as? JetQualifiedExpression
            if (qualified != null) {
                if (qualified.getReceiverExpression()[RECEIVER_VALUE_KEY]) {
                    if (qualified is JetSafeQualifiedExpression) return // already safe
                    val selector = qualified.getSelectorExpression()
                    if (selector != null) {
                        expression = psiFactory.createExpressionByPattern("$0?.$1", receiver, selector)
                        return
                    }
                }
            }

            if (expressionToBeReplaced.isUsedAsExpression(bindingContext)) {
                val thisReplaced = expression.collectDescendantsOfType<JetExpression> { it[RECEIVER_VALUE_KEY] }
                introduceValue(receiver, receiverType, thisReplaced, safeCall = true)
            }
            else {
                expression = psiFactory.createExpressionByPattern("if ($0 != null) { $1 }", receiver, expression)
            }
        }

        private fun ConstructedExpressionWrapper.keepInfixFormIfPossible() {
            val dotQualified = expression as? JetDotQualifiedExpression ?: return
            val receiver = dotQualified.getReceiverExpression()
            if (!receiver[RECEIVER_VALUE_KEY]) return
            val call = dotQualified.getSelectorExpression() as? JetCallExpression ?: return
            val nameExpression = call.getCalleeExpression() as? JetSimpleNameExpression ?: return
            val argument = call.getValueArguments().singleOrNull() ?: return
            if (argument.getArgumentName() != null) return
            val argumentExpression = argument.getArgumentExpression() ?: return
            expression = psiFactory.createExpressionByPattern("$0 ${nameExpression.getText()} $1", receiver, argumentExpression)
        }

        private fun JetExpression?.shouldKeepValue(usageCount: Int): Boolean {
            if (usageCount == 1) return false
            val sideEffectOnly = usageCount == 0

            return when (this) {
                is JetSimpleNameExpression -> false
                is JetQualifiedExpression -> getReceiverExpression().shouldKeepValue(usageCount) || getSelectorExpression().shouldKeepValue(usageCount)
                is JetUnaryExpression -> getOperationToken() in setOf(JetTokens.PLUSPLUS, JetTokens.MINUSMINUS) || getBaseExpression().shouldKeepValue(usageCount)
                is JetStringTemplateExpression -> getEntries().any { if (sideEffectOnly) it.getExpression().shouldKeepValue(usageCount) else it is JetStringTemplateEntryWithExpression }
                is JetThisExpression, is JetSuperExpression, is JetConstantExpression -> false
                is JetParenthesizedExpression -> getExpression().shouldKeepValue(usageCount)
                is JetArrayAccessExpression -> if (sideEffectOnly) getArrayExpression().shouldKeepValue(usageCount) || getIndexExpressions().any { it.shouldKeepValue(usageCount) } else true
                is JetBinaryExpression -> if (sideEffectOnly) getLeft().shouldKeepValue(usageCount) || getRight().shouldKeepValue(usageCount) else true
                is JetIfExpression -> if (sideEffectOnly) getCondition().shouldKeepValue(usageCount) || getThen().shouldKeepValue(usageCount) || getElse().shouldKeepValue(usageCount) else true
                is JetBinaryExpressionWithTypeRHS -> true
                else -> true
            }
        }

        private class Argument(
                val expression: JetExpression,
                val expressionType: JetType?,
                val isNamed: Boolean = false,
                val isDefaultValue: Boolean = false)

        private fun argumentForParameter(
                parameter: ValueParameterDescriptor,
                resolvedCall: ResolvedCall<out CallableDescriptor>,
                bindingContext: BindingContext,
                project: Project): Argument? {
            val resolvedArgument = resolvedCall.getValueArguments()[parameter]!!
            when (resolvedArgument) {
                is ExpressionValueArgument -> {
                    val valueArgument = resolvedArgument.getValueArgument()!!
                    val expression = valueArgument.getArgumentExpression()!!
                    expression.mark(USER_CODE_KEY)
                    if (valueArgument is FunctionLiteralArgument) {
                        expression.mark(WAS_FUNCTION_LITERAL_ARGUMENT_KEY)
                    }
                    return Argument(expression, bindingContext.getType(expression), isNamed = valueArgument.isNamed())
                }

                is DefaultValueArgument -> {
                    val defaultValue = OptionalParametersHelper.defaultParameterValue(parameter, project) ?: return null
                    val (expression, parameterUsages) = defaultValue

                    for ((param, usages) in parameterUsages) {
                        usages.forEach { it.put(ReplaceWithAnnotationAnalyzer.PARAMETER_USAGE_KEY, param.getName()) }
                    }

                    val expressionCopy = expression.copied()

                    // clean up user data in original
                    expression.forEachDescendantOfType<JetExpression> { it.clear(ReplaceWithAnnotationAnalyzer.PARAMETER_USAGE_KEY) }

                    return Argument(expressionCopy, null/*TODO*/, isDefaultValue = true)
                }

                is VarargValueArgument -> {
                    val arguments = resolvedArgument.getArguments()
                    val single = arguments.singleOrNull()
                    if (single != null && single.getSpreadElement() != null) {
                        val expression = single.getArgumentExpression()!!.marked(USER_CODE_KEY)
                        return Argument(expression, bindingContext.getType(expression), isNamed = single.isNamed())
                    }

                    val elementType = parameter.getVarargElementType()!!
                    val expression = JetPsiFactory(project).buildExpression {
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
                    return Argument(expression, parameter.getType(), isNamed = single?.isNamed() ?: false)
                }

                else -> error("Unknown argument type: $resolvedArgument")
            }
        }

        private fun postProcessInsertedExpression(range: PsiChildRange): PsiChildRange {
            val expressions = range.filterIsInstance<JetExpression>().toList()

            expressions.forEach {
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
                    val thisReceiver = (element as? JetQualifiedExpression)?.getReceiverExpression() as? JetThisExpression
                    if (thisReceiver != null && thisReceiver[USER_CODE_KEY]) // don't remove explicit 'this' coming from user's code
                        ShortenReferences.FilterResult.GO_INSIDE
                    else
                        ShortenReferences.FilterResult.PROCESS
                }
            }

            val newExpressions = expressions.map {
                ShortenReferences({ ShortenReferences.Options(removeThis = true) }).process(it, shortenFilter) as JetExpression
            }

            newExpressions.forEach {
                // clean up user data
                it.forEachDescendantOfType<JetExpression> {
                    it.clear(USER_CODE_KEY)
                    it.clear(ReplaceWithAnnotationAnalyzer.PARAMETER_USAGE_KEY)
                    it.clear(ReplaceWithAnnotationAnalyzer.TYPE_PARAMETER_USAGE_KEY)
                    it.clear(PARAMETER_VALUE_KEY)
                    it.clear(RECEIVER_VALUE_KEY)
                    it.clear(WAS_FUNCTION_LITERAL_ARGUMENT_KEY)
                }
                it.forEachDescendantOfType<JetValueArgument> {
                    it.clear(MAKE_ARGUMENT_NAMED_KEY)
                    it.clear(DEFAULT_PARAMETER_VALUE_KEY)
                }
            }

            return PsiChildRange(newExpressions.first(), newExpressions.last())
        }

        private fun introduceNamedArguments(result: JetExpression) {
            val callsToProcess = LinkedHashSet<JetCallExpression>()
            result.forEachDescendantOfType<JetValueArgument> {
                if (it[MAKE_ARGUMENT_NAMED_KEY] && !it.isNamed()) {
                    val callExpression = (it.getParent() as? JetValueArgumentList)?.getParent() as? JetCallExpression
                    callsToProcess.addIfNotNull(callExpression)
                }
            }

            val psiFactory = JetPsiFactory(result)

            for (callExpression in callsToProcess) {
                val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return
                if (!resolvedCall.getStatus().isSuccess()) return

                val argumentsToMakeNamed = callExpression.getValueArguments().dropWhile { !it[MAKE_ARGUMENT_NAMED_KEY] }
                for (argument in argumentsToMakeNamed) {
                    if (argument.isNamed()) continue
                    if (argument is JetFunctionLiteralArgument) continue
                    val argumentMatch = resolvedCall.getArgumentMapping(argument) as ArgumentMatch
                    val name = argumentMatch.valueParameter.getName()
                    //TODO: not always correct for vararg's
                    val newArgument = psiFactory.createArgument(argument.getArgumentExpression()!!, name, argument.getSpreadElement() != null)

                    if (argument[DEFAULT_PARAMETER_VALUE_KEY]) {
                        newArgument.mark(DEFAULT_PARAMETER_VALUE_KEY)
                    }

                    argument.replace(newArgument)
                }
            }
        }

        private fun dropArgumentsForDefaultValues(result: JetExpression) {
            val project = result.getProject()
            val newBindingContext = result.analyze()
            val argumentsToDrop = ArrayList<ValueArgument>()

            // we drop only those arguments that added to the code from some parameter's default
            fun canDropArgument(argument: ValueArgument) = (argument as JetValueArgument)[DEFAULT_PARAMETER_VALUE_KEY]

            result.forEachDescendantOfType<JetCallExpression> { callExpression ->
                val resolvedCall = callExpression.getResolvedCall(newBindingContext) ?: return@forEachDescendantOfType

                argumentsToDrop.addAll(OptionalParametersHelper.detectArgumentsToDropForDefaults(resolvedCall, project, ::canDropArgument))
            }

            for (argument in argumentsToDrop) {
                argument as JetValueArgument
                val argumentList = argument.getParent() as JetValueArgumentList
                argumentList.removeArgument(argument)
                if (argumentList.getArguments().isEmpty()) {
                    val callExpression = argumentList.getParent() as JetCallExpression
                    if (callExpression.getFunctionLiteralArguments().isNotEmpty()) {
                        argumentList.delete()
                    }
                }
            }
        }

        private fun arrayOfFunctionName(elementType: JetType): String {
            return when {
                KotlinBuiltIns.isInt(elementType) -> "kotlin.intArrayOf"
                KotlinBuiltIns.isLong(elementType) -> "kotlin.longArrayOf"
                KotlinBuiltIns.isShort(elementType) -> "kotlin.shortArrayOf"
                KotlinBuiltIns.isChar(elementType) -> "kotlin.charArrayOf"
                KotlinBuiltIns.isBoolean(elementType) -> "kotlin.booleanArrayOf"
                KotlinBuiltIns.isByte(elementType) -> "kotlin.byteArrayOf"
                KotlinBuiltIns.isDouble(elementType) -> "kotlin.doubleArrayOf"
                KotlinBuiltIns.isFloat(elementType) -> "kotlin.floatArrayOf"
                elementType.isError() -> "kotlin.arrayOf"
                else -> "kotlin.arrayOf<" + IdeDescriptorRenderers.SOURCE_CODE.renderType(elementType) + ">"
            }
        }

        private fun removeExplicitTypeArguments(result: JetExpression) {
            result.collectDescendantsOfType<JetTypeArgumentList>(canGoInside = { !it[USER_CODE_KEY] }) {
                RemoveExplicitTypeArgumentsIntention.isApplicableTo(it, approximateFlexible = true)
            }.forEach { it.delete() }
        }

        private fun simplifySpreadArrayOfArguments(expression: JetExpression) {
            //TODO: test for nested

            val argumentsToExpand = ArrayList<Pair<JetValueArgument, Collection<JetValueArgument>>>()

            expression.forEachDescendantOfType<JetValueArgument>(canGoInside = { !it[USER_CODE_KEY] }) { argument ->
                if (argument.getSpreadElement() != null && !argument.isNamed()) {
                    val argumentExpression = argument.getArgumentExpression() ?: return@forEachDescendantOfType
                    val resolvedCall = argumentExpression.getResolvedCall(argumentExpression.analyze(BodyResolveMode.PARTIAL)) ?: return@forEachDescendantOfType
                    val callExpression = resolvedCall.getCall().getCallElement() as? JetCallExpression ?: return@forEachDescendantOfType
                    if (CompileTimeConstantUtils.isArrayMethodCall(resolvedCall)) {
                        argumentsToExpand.add(argument to callExpression.getValueArguments())
                    }
                }
            }

            for ((argument, replacements) in argumentsToExpand) {
                argument.replaceByMultiple(replacements)
            }
        }

        private fun JetValueArgument.replaceByMultiple(arguments: Collection<JetValueArgument>) {
            val list = getParent() as JetValueArgumentList
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

        private fun restoreFunctionLiteralArguments(expression: JetExpression) {
            val callExpressions = ArrayList<JetCallExpression>()

            expression.forEachDescendantOfType<JetExpression>( fun (expr: JetExpression) {
                if (!expr[WAS_FUNCTION_LITERAL_ARGUMENT_KEY]) return
                assert(expr.unpackFunctionLiteral() != null)

                val argument = expr.getParent() as? JetValueArgument ?: return
                if (argument is JetFunctionLiteralArgument) return
                if (argument.isNamed()) return
                val argumentList = argument.getParent() as? JetValueArgumentList ?: return
                if (argument != argumentList.getArguments().last()) return
                val callExpression = argumentList.getParent() as? JetCallExpression ?: return
                if (callExpression.getFunctionLiteralArguments().isNotEmpty()) return

                val resolvedCall = callExpression.getResolvedCall(callExpression.analyze(BodyResolveMode.PARTIAL)) ?: return
                if (!resolvedCall.getStatus().isSuccess()) return
                val argumentMatch = resolvedCall.getArgumentMapping(argument) as ArgumentMatch
                if (argumentMatch.valueParameter != resolvedCall.getResultingDescriptor().getValueParameters().last()) return

                callExpressions.add(callExpression)
            })

            callExpressions.forEach { it.moveFunctionLiteralOutsideParentheses() }
        }

        //TODO: making functions below private causes VerifyError
        fun <T: Any> PsiElement.get(key: Key<T>): T? = getCopyableUserData(key)
        fun PsiElement.get(key: Key<Unit>): Boolean = getCopyableUserData(key) != null
        fun <T: Any> JetElement.clear(key: Key<T>) = putCopyableUserData(key, null)
        fun <T: Any> JetElement.put(key: Key<T>, value: T) = putCopyableUserData(key, value)
        fun JetElement.mark(key: Key<Unit>) = putCopyableUserData(key, Unit)

        fun <T: JetElement> T.marked(key: Key<Unit>): T {
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
    }

    class ConstructedExpressionWrapper(
            var expression: JetExpression,
            val expressionToBeReplaced: JetExpression,
            val bindingContext: BindingContext
    ) {
        val addedStatements = ArrayList<JetExpression>()

        val psiFactory = JetPsiFactory(expressionToBeReplaced)

        public fun replaceExpression(oldExpression: JetExpression, newExpression: JetExpression): JetExpression {
            assert(expression.isAncestor(oldExpression))
            val result = oldExpression.replace(newExpression) as JetExpression
            if (oldExpression == expression) {
                expression = result
            }
            return result
        }

        public fun introduceValue(
                value: JetExpression,
                valueType: JetType?,
                usages: Collection<JetExpression>,
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
                    KotlinNameSuggester.suggestNamesByExpressionOnly(value, validator, "t").first()
                return Name.identifier(name)
            }

            // checks that name is used (without receiver) inside expression being constructed but not inside usages that will be replaced
            fun isNameUsed(name: String) = collectNameUsages(expression, name).any { nameUsage -> usages.none { it.isAncestor(nameUsage) } }

            if (!safeCall) {
                val block = expressionToBeReplaced.getParent() as? JetBlockExpression
                if (block != null) {
                    val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expressionToBeReplaced]

                    if (usages.isNotEmpty()) {
                        var explicitType: JetType? = null
                        if (valueType != null && !ErrorUtils.containsErrorType(valueType)) {
                            val valueTypeWithoutExpectedType = value.analyzeInContext(
                                    resolutionScope!!,
                                    dataFlowInfo = bindingContext.getDataFlowInfo(expressionToBeReplaced)
                            ).getType(value)
                            if (valueTypeWithoutExpectedType == null || ErrorUtils.containsErrorType(valueTypeWithoutExpectedType)) {
                                explicitType = valueType
                            }
                        }

                        val name = suggestName { name ->
                            resolutionScope!!.getLocalVariable(Name.identifier(name)) == null && !isNameUsed(name)
                        }

                        var declaration = psiFactory.createDeclarationByPattern<JetVariableDeclaration>("val $0 = $1", name, value)
                        declaration = block.addBefore(declaration, expressionToBeReplaced) as JetVariableDeclaration
                        block.addBefore(psiFactory.createNewLine(), expressionToBeReplaced)

                        if (explicitType != null) {
                            declaration.setType(explicitType)
                        }

                        replaceUsages(name)

                        addedStatements.add(declaration)
                    }
                    else {
                        addedStatements.add(block.addBefore(value, expressionToBeReplaced) as JetExpression)
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

        private fun collectNameUsages(scope: JetExpression, name: String)
                = scope.collectDescendantsOfType<JetSimpleNameExpression> { it.getReceiverExpression() == null && it.getReferencedName() == name }
    }
}
