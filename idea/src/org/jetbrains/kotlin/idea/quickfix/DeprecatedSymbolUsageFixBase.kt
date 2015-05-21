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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.OptionalParametersHelper
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.core.refactoring.JetNameValidator
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.renderName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import java.util.ArrayList

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
        if (replaceWithPattern(descriptor) != replaceWith) return false

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

        val replacement = ReplaceWithAnnotationAnalyzer.analyze(replaceWith, descriptor, element.getResolutionFacade(), file, project)

        invoke(resolvedCall, bindingContext, replacement, project, editor)
    }

    protected abstract fun invoke(
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            bindingContext: BindingContext,
            replacement: ReplaceWithAnnotationAnalyzer.ReplacementExpression,
            project: Project,
            editor: Editor?)

    companion object {
        public fun replaceWithPattern(descriptor: DeclarationDescriptor): ReplaceWith? {
            val annotationClass = descriptor.builtIns.getDeprecatedAnnotation()
            val annotation = descriptor.getAnnotations().findAnnotation(DescriptorUtils.getFqNameSafe(annotationClass))!!
            val replaceWithValue = annotation.getAllValueArguments().entrySet()
                                           .singleOrNull { it.key.getName().asString() == "replaceWith"/*TODO*/ }
                                           ?.value?.getValue() as? AnnotationDescriptor ?: return null
            val pattern = replaceWithValue.getAllValueArguments().entrySet()
                                  .singleOrNull { it.key.getName().asString() == "expression"/*TODO*/ }
                                  ?.value?.getValue() as? String ?: return null
            if (pattern.isEmpty()) return null
            val argument = replaceWithValue.getAllValueArguments().entrySet().singleOrNull { it.key.getName().asString() == "imports"/*TODO*/ }?.value
            val imports = (argument?.getValue() as? List<CompileTimeConstant<String>>)?.map { it.getValue() } ?: emptyList()
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
            val qualifiedExpression = callExpression.getParent() as? JetQualifiedExpression
            val expressionToReplace = qualifiedExpression ?: callExpression

            var receiver = element.getReceiverExpression()?.marked(USER_CODE_KEY)
            var receiverType = if (receiver != null) bindingContext.getType(receiver) else null

            if (receiver == null) {
                val receiverValue = if (descriptor.isExtension) resolvedCall.getExtensionReceiver() else resolvedCall.getDispatchReceiver()
                val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expressionToReplace]
                if (receiverValue is ThisReceiver && resolutionScope != null) {
                    receiver = receiverValue.asExpression(resolutionScope, psiFactory)
                    receiverType = receiverValue.getType()
                }
            }

            receiver?.mark(RECEIVER_VALUE_KEY)

            for ((parameter, usages) in replacement.parameterUsages.entrySet()) {
                usages.forEach { it.put(PARAMETER_USAGE_KEY, parameter) }
            }

            var expression = replacement.expression

            //TODO: this@
            for (thisExpression in expression.collectDescendantsOfType<JetThisExpression>()) {
                if (receiver != null) {
                    thisExpression.replace(receiver)
                }
                else {
                    thisExpression.mark(RECEIVER_VALUE_KEY)
                }
            }

            @data class IntroduceValueForParameter(
                    val parameter: ValueParameterDescriptor,
                    val value: JetExpression,
                    val valueType: JetType?)

            val introduceValuesForParameters = ArrayList<IntroduceValueForParameter>()

            // process parameters in reverse order because default values can use previous parameters
            for (parameter in descriptor.getValueParameters().reverse()) {
                val argument = argumentForParameter(parameter, resolvedCall, bindingContext, project) ?: continue

                argument.expression.put(PARAMETER_VALUE_KEY, parameter)

                val originalParameter = parameter.getOriginal()
                val usages = expression.collectDescendantsOfType<JetExpression> { it[PARAMETER_USAGE_KEY] == originalParameter }
                usages.forEach { it.replace(argument.wrapped) }

                if (argument.expression.shouldKeepValue(usages.size())) {
                    introduceValuesForParameters.add(IntroduceValueForParameter(parameter, argument.expression, argument.expressionType))
                }
            }

            unwrapDefaultValues(expression)

            if (qualifiedExpression is JetSafeQualifiedExpression) {
                expression = expression.wrapExpressionForSafeCall(expressionToReplace, receiver!!, receiverType, bindingContext)
            }
            else if (callExpression is JetBinaryExpression && callExpression.getOperationToken() == JetTokens.IDENTIFIER) {
                expression = expression.keepInfixFormIfPossible()
            }

            if (receiver != null) {
                val thisReplaced = expression.collectDescendantsOfType<JetExpression> { it[RECEIVER_VALUE_KEY] }
                if (receiver.shouldKeepValue(thisReplaced.size())) {
                    expression = expression.introduceValue(receiver, receiverType, expressionToReplace, bindingContext, thisReplaced)
                }
            }

            for ((parameter, value, valueType) in introduceValuesForParameters) {
                val usagesReplaced = expression.collectDescendantsOfType<JetExpression> { it[PARAMETER_VALUE_KEY] == parameter }
                expression = expression.introduceValue(value, valueType, expressionToReplace, bindingContext, usagesReplaced, nameSuggestion = parameter.getName().asString())
            }

            var result = expressionToReplace.replace(expression) as JetExpression

            //TODO: drop import of old function (if not needed anymore)?

            val file = result.getContainingJetFile()
            for (importFqName in replacement.imports) {
                val descriptors = file.getResolutionFacade().resolveImportReference(file, importFqName)
                val descriptorToImport = descriptors.firstOrNull() ?: continue
                ImportInsertHelper.getInstance(project).importDescriptor(file, descriptorToImport)
            }

            //TODO: do this earlier
            dropArgumentsForDefaultValues(result)

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
            result = ShortenReferences({ ShortenReferences.Options(removeThis = true) }).process(result, shortenFilter) as JetExpression

            // clean up user data
            result.forEachDescendantOfType<JetExpression> {
                it.clear(USER_CODE_KEY)
                it.clear(PARAMETER_USAGE_KEY)
                it.clear(PARAMETER_VALUE_KEY)
                it.clear(RECEIVER_VALUE_KEY)
                it.clear(DEFAULT_PARAMETER_VALUE_KEY)
            }

            return result
        }

        private fun JetExpression.wrapExpressionForSafeCall(
                expressionToReplace: JetExpression,
                receiver: JetExpression,
                receiverType: JetType?,
                bindingContext: BindingContext
        ): JetExpression {
            val psiFactory = JetPsiFactory(this)
            val qualified = this as? JetQualifiedExpression
            if (qualified != null) {
                if (qualified.getReceiverExpression()[RECEIVER_VALUE_KEY]) {
                    if (qualified is JetSafeQualifiedExpression) return this // already safe
                    val selector = qualified.getSelectorExpression()
                    if (selector != null) {
                        return psiFactory.createExpressionByPattern("$0?.$1", receiver, selector)
                    }
                }
            }

            if (expressionToReplace.isUsedAsExpression(bindingContext)) {
                val thisReplaced = this.collectDescendantsOfType<JetExpression> { it[RECEIVER_VALUE_KEY] }
                return this.introduceValue(receiver, receiverType, expressionToReplace, bindingContext, thisReplaced, safeCall = true)
            }
            else {
                return psiFactory.createExpressionByPattern("if ($0 != null) { $1 }", receiver, this)
            }
        }

        private fun JetExpression.keepInfixFormIfPossible(): JetExpression {
            if (this !is JetDotQualifiedExpression) return this
            val receiver = getReceiverExpression()
            if (!receiver[RECEIVER_VALUE_KEY]) return this
            val call = getSelectorExpression() as? JetCallExpression ?: return this
            val nameExpression = call.getCalleeExpression() as? JetSimpleNameExpression ?: return this
            val argument = call.getValueArguments().singleOrNull() ?: return this
            if (argument.getArgumentName() != null) return this
            val argumentExpression = argument.getArgumentExpression() ?: return this
            return JetPsiFactory(this).createExpressionByPattern("$0 ${nameExpression.getText()} $1", receiver, argumentExpression)
        }

        private fun JetExpression.introduceValue(
                value: JetExpression,
                valueType: JetType?,
                insertDeclarationsBefore: JetExpression,
                bindingContext: BindingContext,
                usages: Collection<JetExpression>,
                nameSuggestion: String? = null,
                safeCall: Boolean = false
        ): JetExpression {
            assert(usages.all { isAncestor(it, strict = true) })

            val psiFactory = JetPsiFactory(this)

            fun nameInCode(name: String) = Name.identifier(name).renderName()

            fun replaceUsages(name: String) {
                val nameInCode = psiFactory.createExpression(nameInCode(name))
                for (usage in usages) {
                    usage.replace(nameInCode)
                }
            }

            fun suggestName(validator: JetNameValidator): String {
                return if (nameSuggestion != null)
                    validator.validateName(nameSuggestion)
                else
                    JetNameSuggester.suggestNamesForExpression(value, validator, "t").first()
            }

            // checks that name is used (without receiver) inside this expression but not inside usages that will be replaced
            fun isNameUsed(name: String) = collectNameUsages(this, name).any { nameUsage -> usages.none { it.isAncestor(nameUsage) } }

            if (!safeCall) {
                val block = insertDeclarationsBefore.getParent() as? JetBlockExpression
                if (block != null) {
                    val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, insertDeclarationsBefore]

                    if (usages.isNotEmpty()) {
                        var explicitType: JetType? = null
                        if (valueType != null && !ErrorUtils.containsErrorType(valueType)) {
                            val valueTypeWithoutExpectedType = value.analyzeInContext(
                                    resolutionScope,
                                    dataFlowInfo = bindingContext.getDataFlowInfo(insertDeclarationsBefore)
                            ).getType(value)
                            if (valueTypeWithoutExpectedType == null || ErrorUtils.containsErrorType(valueTypeWithoutExpectedType)) {
                                explicitType = valueType
                            }
                        }

                        val name = suggestName(object : JetNameValidator() {
                            override fun validateInner(name: String): Boolean {
                                return resolutionScope.getLocalVariable(Name.identifier(name)) == null && !isNameUsed(name)
                            }
                        })

                        var declaration = psiFactory.createDeclaration<JetVariableDeclaration>("val ${nameInCode(name)} = " + value.getText())
                        declaration = block.addBefore(declaration, insertDeclarationsBefore) as JetVariableDeclaration
                        block.addBefore(psiFactory.createNewLine(), insertDeclarationsBefore)

                        if (explicitType != null) {
                            declaration.setType(explicitType)
                        }

                        replaceUsages(name)
                    }
                    else {
                        block.addBefore(value, insertDeclarationsBefore)
                        block.addBefore(psiFactory.createNewLine(), insertDeclarationsBefore)
                    }
                    return this
                }
            }

            val dot = if (safeCall) "?." else "."

            if (!isNameUsed("it")) {
                replaceUsages("it")
                return psiFactory.createExpressionByPattern("$0${dot}let { $1 }", value, this)
            }
            else {
                val name = suggestName(object : JetNameValidator() {
                    override fun validateInner(name: String) = !isNameUsed(name)
                })
                replaceUsages(name)
                return psiFactory.createExpressionByPattern("$0${dot}let { ${nameInCode(name)} -> $1 }", value, this)
            }
        }

        private fun collectNameUsages(scope: JetExpression, name: String)
                = scope.collectDescendantsOfType<JetSimpleNameExpression> { it.getReceiverExpression() == null && it.getReferencedName() == name }

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

            // TODO: discuss it
                is JetBinaryExpression -> if (sideEffectOnly) getLeft().shouldKeepValue(usageCount) || getRight().shouldKeepValue(usageCount) else true
                is JetIfExpression -> if (sideEffectOnly) getCondition().shouldKeepValue(usageCount) || getThen().shouldKeepValue(usageCount) || getElse().shouldKeepValue(usageCount) else true
                is JetBinaryExpressionWithTypeRHS -> true

                else -> true // what else it can be?
            }
        }

        private class Argument(
                val expression: JetExpression,
                val wrapped: JetExpression,
                val expressionType: JetType?,
                val isDefaultValue: Boolean)

        private fun argumentForParameter(
                parameter: ValueParameterDescriptor,
                resolvedCall: ResolvedCall<out CallableDescriptor>,
                bindingContext: BindingContext,
                project: Project): Argument? {
            //TODO: named parameters - keep named form if makes sense
            //TODO: keep functional literal argument form
            val resolvedArgument = resolvedCall.getValueArguments()[parameter]!!
            when (resolvedArgument) {
                is ExpressionValueArgument -> {
                    val expression = resolvedArgument.getValueArgument()!!.getArgumentExpression()!!
                    expression.mark(USER_CODE_KEY)
                    return Argument(expression, expression, bindingContext.getType(expression), isDefaultValue = false)
                }

                is DefaultValueArgument -> {
                    val defaultValue = OptionalParametersHelper.defaultParameterValue(parameter, project) ?: return null
                    val (expression, parameterUsages) = defaultValue

                    for ((param, usages) in parameterUsages) {
                        usages.forEach { it.put(PARAMETER_USAGE_KEY, param) }
                    }

                    // we temporary wrap default values into parenthesis so that we can safely mark them with DEFAULT_PARAMETER_VALUE_KEY
                    val wrapped = JetPsiFactory(project).createExpressionByPattern("($0)", expression) as JetParenthesizedExpression
                    wrapped.mark(DEFAULT_PARAMETER_VALUE_KEY)

                    // clean up user data in original
                    expression.forEachDescendantOfType<JetExpression> { it.clear(PARAMETER_USAGE_KEY) }

                    return Argument(wrapped.getExpression()!!, wrapped, null/*TODO*/, isDefaultValue = true)
                }

                is VarargValueArgument -> /*TODO*/ return null

                else -> error("Unknown argument type: $resolvedArgument")
            }
        }

        private fun dropArgumentsForDefaultValues(result: JetExpression) {
            val project = result.getProject()
            val newBindingContext = result.analyze()
            val argumentsToDrop = ArrayList<ValueArgument>()

            // we drop only those arguments that added to the code from some parameter's default
            fun canDropArgument(argument: ValueArgument) = argument.getArgumentExpression()!![DEFAULT_PARAMETER_VALUE_KEY]

            //TODO: other types of calls
            result.forEachDescendantOfType<JetCallExpression> { callExpression ->
                val resolvedCall = callExpression.getResolvedCall(newBindingContext) ?: return@forEachDescendantOfType

                argumentsToDrop.addAll(OptionalParametersHelper.detectArgumentsToDropForDefaults(resolvedCall, project, ::canDropArgument))
            }

            for (argument in argumentsToDrop) {
                argument as JetValueArgument
                val argumentList = argument.getParent() as JetValueArgumentList
                argumentList.removeArgument(argument)
            }
        }

        private fun unwrapDefaultValues(expression: JetExpression) {
            val values = expression.collectDescendantsOfType<JetParenthesizedExpression> {
                it[DEFAULT_PARAMETER_VALUE_KEY] && !it.getParent()[DEFAULT_PARAMETER_VALUE_KEY]
            }

            fun JetParenthesizedExpression.unwrap(): JetExpression {
                if (!this[DEFAULT_PARAMETER_VALUE_KEY]) return this
                var inner = getExpression()!!
                if (inner is JetParenthesizedExpression) {
                    inner = inner.unwrap()
                }
                val result = replace(inner) as JetExpression
                result.mark(DEFAULT_PARAMETER_VALUE_KEY)
                return result
            }

            values.forEach { it.unwrap() }
        }

        //TODO: making functions below private causes VerifyError
        fun <T: Any> PsiElement.get(key: Key<T>): T? = getCopyableUserData(key)
        fun PsiElement.get(key: Key<Unit>): Boolean = getCopyableUserData(key) != null
        fun <T: Any> JetExpression.clear(key: Key<T>) = putCopyableUserData(key, null)
        fun <T: Any> JetExpression.put(key: Key<T>, value: T) = putCopyableUserData(key, value)
        fun JetExpression.mark(key: Key<Unit>) = putCopyableUserData(key, Unit)
        fun <T: JetExpression> T.marked(key: Key<Unit>): T {
            putCopyableUserData(key, Unit)
            return this
        }

        private val USER_CODE_KEY = Key<Unit>("USER_CODE")
        private val PARAMETER_USAGE_KEY = Key<ValueParameterDescriptor>("PARAMETER_USAGE")
        private val PARAMETER_VALUE_KEY = Key<ValueParameterDescriptor>("PARAMETER_VALUE")
        private val RECEIVER_VALUE_KEY = Key<Unit>("RECEIVER_VALUE")
        private val DEFAULT_PARAMETER_VALUE_KEY = Key<Unit>("DEFAULT_PARAMETER_VALUE")
    }
}

