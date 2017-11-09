/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class CodeInliner<TCallElement : KtElement>(
        private val nameExpression: KtSimpleNameExpression,
        private val bindingContext: BindingContext,
        private val resolvedCall: ResolvedCall<out CallableDescriptor>,
        private val callElement: TCallElement,
        codeToInline: CodeToInline
) {
    private val codeToInline = codeToInline.toMutable()
    private val project = nameExpression.project
    private val psiFactory = KtPsiFactory(project)

    fun doInline(): KtElement? {
        val descriptor = resolvedCall.resultingDescriptor
        val file = nameExpression.containingKtFile

        val qualifiedElement = if (callElement is KtExpression) callElement.getQualifiedExpressionForSelectorOrThis() else callElement
        val assignment = (qualifiedElement as? KtExpression)
                ?.getAssignmentByLHS()
                ?.takeIf { it.operationToken == KtTokens.EQ }
        val elementToBeReplaced = assignment ?: qualifiedElement
        val callableForParameters = if (assignment != null && descriptor is PropertyDescriptor)
            descriptor.setter ?: descriptor
        else
            descriptor

        val commentSaver = CommentSaver(elementToBeReplaced, saveLineBreaks = true)

        // if the value to be inlined is not used and has no side effects we may drop it
        if (codeToInline.mainExpression != null
            && elementToBeReplaced is KtExpression
            && !elementToBeReplaced.isUsedAsExpression(bindingContext)
            && !codeToInline.mainExpression.shouldKeepValue(usageCount = 0)
        ) {
            codeToInline.mainExpression = null
        }

        var receiver = nameExpression.getReceiverExpression()?.marked(USER_CODE_KEY)
        var receiverType = if (receiver != null) bindingContext.getType(receiver) else null

        if (receiver == null) {
            val receiverValue = if (descriptor.isExtension) resolvedCall.extensionReceiver else resolvedCall.dispatchReceiver
            if (receiverValue is ImplicitReceiver) {
                val resolutionScope = elementToBeReplaced.getResolutionScope(bindingContext, elementToBeReplaced.getResolutionFacade())
                receiver = receiverValue.asExpression(resolutionScope, psiFactory)
                receiverType = receiverValue.type
            }
        }

        receiver?.mark(RECEIVER_VALUE_KEY)

        for (thisExpression in codeToInline.collectDescendantsOfType<KtThisExpression>()) {
            // for this@ClassName we have only option to keep it as is (although it's sometimes incorrect but we have no other options)
            if (thisExpression.labelQualifier == null && receiver != null) {
                codeToInline.replaceExpression(thisExpression, receiver)
            }
        }

        val introduceValuesForParameters = processValueParameterUsages(callableForParameters)

        processTypeParameterUsages()

        val lexicalScope = callElement.parent.getResolutionScope(bindingContext)

        if (elementToBeReplaced is KtSafeQualifiedExpression) {
            wrapCodeForSafeCall(receiver!!, receiverType, elementToBeReplaced)
        }
        else if (callElement is KtBinaryExpression && callElement.operationToken == KtTokens.IDENTIFIER) {
            keepInfixFormIfPossible()
        }

        if (elementToBeReplaced is KtExpression) {
            if (receiver != null) {
                val thisReplaced = codeToInline.collectDescendantsOfType<KtExpression> { it[RECEIVER_VALUE_KEY] }
                if (receiver.shouldKeepValue(usageCount = thisReplaced.size)) {
                    codeToInline.introduceValue(receiver, receiverType, thisReplaced, elementToBeReplaced)
                }
            }

            for ((parameter, value, valueType) in introduceValuesForParameters) {
                val usagesReplaced = codeToInline.collectDescendantsOfType<KtExpression> { it[PARAMETER_VALUE_KEY] == parameter }
                codeToInline.introduceValue(value, valueType, usagesReplaced, elementToBeReplaced, nameSuggestion = parameter.name.asString())
            }
        }

        codeToInline.fqNamesToImport
                .flatMap { file.resolveImportReference(it) }
                .forEach { ImportInsertHelper.getInstance(project).importDescriptor(file, it) }

        val replacementPerformer = when (elementToBeReplaced) {
            is KtExpression -> ExpressionReplacementPerformer(codeToInline, elementToBeReplaced)
            is KtAnnotationEntry -> AnnotationEntryReplacementPerformer(codeToInline, elementToBeReplaced)
            else -> error("Unsupported element")
        }

        return replacementPerformer.doIt(postProcessing = { range ->
            val newRange = postProcessInsertedCode(range, lexicalScope)
            if (!newRange.isEmpty) {
                commentSaver.restore(newRange)
            }
            newRange
        })
    }

    private fun renameDuplicates(declarations: List<KtNamedDeclaration>,
                                 lexicalScope: LexicalScope) {

        val validator = CollectingNameValidator { !it.nameHasConflictsInScope(lexicalScope) }
        for (declaration in declarations) {
            val oldName = declaration.name
            if (oldName != null && oldName.nameHasConflictsInScope(lexicalScope)) {
                val newName = KotlinNameSuggester.suggestNameByName(oldName, validator)
                val renameProcessor = RenameProcessor(project, declaration, newName, false, false)
                renameProcessor.run()
            }
        }
    }

    private fun processValueParameterUsages(descriptor: CallableDescriptor): Collection<IntroduceValueForParameter> {
        val introduceValuesForParameters = ArrayList<IntroduceValueForParameter>()

        // process parameters in reverse order because default values can use previous parameters
        for (parameter in descriptor.valueParameters.asReversed()) {
            val argument = argumentForParameter(parameter, descriptor) ?: continue

            argument.expression.put(PARAMETER_VALUE_KEY, parameter)

            val parameterName = parameter.name
            val usages = codeToInline.collectDescendantsOfType<KtExpression> {
                it[CodeToInline.PARAMETER_USAGE_KEY] == parameterName
            }
            usages.forEach {
                val usageArgument = it.parent as? KtValueArgument
                if (argument.isNamed) {
                    usageArgument?.mark(MAKE_ARGUMENT_NAMED_KEY)
                }
                if (argument.isDefaultValue) {
                    usageArgument?.mark(DEFAULT_PARAMETER_VALUE_KEY)
                }
                codeToInline.replaceExpression(it, argument.expression)
            }

            //TODO: sometimes we need to add explicit type arguments here because we don't have expected type in the new context

            if (argument.expression.shouldKeepValue(usageCount = usages.size)) {
                introduceValuesForParameters.add(IntroduceValueForParameter(parameter, argument.expression, argument.expressionType))
            }
        }

        return introduceValuesForParameters
    }

    private data class IntroduceValueForParameter(
            val parameter: ValueParameterDescriptor,
            val value: KtExpression,
            val valueType: KotlinType?)

    private fun processTypeParameterUsages() {
        val typeParameters = resolvedCall.resultingDescriptor.original.typeParameters

        val callElement = resolvedCall.call.callElement
        val callExpression = callElement as? KtCallExpression
        val explicitTypeArgs = callExpression?.typeArgumentList?.arguments
        if (explicitTypeArgs != null && explicitTypeArgs.size != typeParameters.size) return

        for ((index, typeParameter) in typeParameters.withIndex()) {
            val parameterName = typeParameter.name
            val usages = codeToInline.collectDescendantsOfType<KtExpression> {
                it[CodeToInline.TYPE_PARAMETER_USAGE_KEY] == parameterName
            }

            val type = resolvedCall.typeArguments[typeParameter]!!
            val typeElement = if (explicitTypeArgs != null) { // we use explicit type arguments if available to avoid shortening
                val _typeElement = explicitTypeArgs[index].typeReference?.typeElement ?: continue
                _typeElement.marked(USER_CODE_KEY)
            }
            else {
                psiFactory.createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)).typeElement!!
            }

            val typeClassifier = type.constructor.declarationDescriptor

            for (usage in usages) {
                val parent = usage.parent
                if (parent is KtClassLiteralExpression && typeClassifier != null) {
                    // for class literal ("X::class") we need type arguments only for kotlin.Array
                    val arguments =
                            if (typeElement is KtUserType && KotlinBuiltIns.isArray(type)) typeElement.typeArgumentList?.text.orEmpty()
                            else ""
                    codeToInline.replaceExpression(usage, psiFactory.createExpression(
                            IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(typeClassifier) + arguments
                    ))
                }
                else if (parent is KtUserType) {
                    parent.replace(typeElement)
                }
                else {
                    //TODO: tests for this?
                    codeToInline.replaceExpression(usage, psiFactory.createExpression(typeElement.text))
                }
            }
        }
    }

    private fun wrapCodeForSafeCall(receiver: KtExpression, receiverType: KotlinType?, expressionToBeReplaced: KtExpression) {
        if (codeToInline.statementsBefore.isEmpty()) {
            val qualified = codeToInline.mainExpression as? KtQualifiedExpression
            if (qualified != null) {
                if (qualified.receiverExpression[RECEIVER_VALUE_KEY]) {
                    if (qualified is KtSafeQualifiedExpression) return // already safe
                    val selector = qualified.selectorExpression
                    if (selector != null) {
                        codeToInline.mainExpression = psiFactory.createExpressionByPattern("$0?.$1", receiver, selector)
                        return
                    }
                }
            }
        }

        if (expressionToBeReplaced.isUsedAsExpression(bindingContext)) {
            val thisReplaced = codeToInline.collectDescendantsOfType<KtExpression> { it[RECEIVER_VALUE_KEY] }
            codeToInline.introduceValue(receiver, receiverType, thisReplaced, expressionToBeReplaced, safeCall = true)
        }
        else {
            val ifExpression = psiFactory.buildExpression {
                appendFixedText("if (")
                appendExpression(receiver)
                appendFixedText("!=null) {")
                codeToInline.statementsBefore.forEach {
                    appendExpression(it)
                    appendFixedText("\n")
                }
                codeToInline.mainExpression?.let {
                    appendExpression(it)
                    appendFixedText("\n")
                }
                appendFixedText("}")
            }
            codeToInline.mainExpression = ifExpression
            codeToInline.statementsBefore.clear()
        }
    }

    private fun keepInfixFormIfPossible() {
        if (codeToInline.statementsBefore.isNotEmpty()) return
        val dotQualified = codeToInline.mainExpression as? KtDotQualifiedExpression ?: return
        val receiver = dotQualified.receiverExpression
        if (!receiver[RECEIVER_VALUE_KEY]) return
        val call = dotQualified.selectorExpression as? KtCallExpression ?: return
        val nameExpression = call.calleeExpression as? KtSimpleNameExpression ?: return
        val argument = call.valueArguments.singleOrNull() ?: return
        if (argument.getArgumentName() != null) return
        val argumentExpression = argument.getArgumentExpression() ?: return
        codeToInline.mainExpression = psiFactory.createExpressionByPattern("$0 ${nameExpression.text} $1", receiver, argumentExpression)
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
            null -> false
            else -> true
        }
    }

    private class Argument(
            val expression: KtExpression,
            val expressionType: KotlinType?,
            val isNamed: Boolean = false,
            val isDefaultValue: Boolean = false)

    private fun argumentForParameter(parameter: ValueParameterDescriptor, callableDescriptor: CallableDescriptor): Argument? {
        if (callableDescriptor is PropertySetterDescriptor) {
            val valueAssigned = (callElement as? KtExpression)
                    ?.getQualifiedExpressionForSelectorOrThis()
                    ?.getAssignmentByLHS()
                    ?.right ?: return null
            return Argument(valueAssigned, bindingContext.getType(valueAssigned))
        }

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
                val (defaultValue, parameterUsages) = OptionalParametersHelper.defaultParameterValue(parameter, project) ?: return null

                for ((param, usages) in parameterUsages) {
                    usages.forEach { it.put(CodeToInline.PARAMETER_USAGE_KEY, param.name) }
                }

                val defaultValueCopy = defaultValue.copied()

                // clean up user data in original
                defaultValue.forEachDescendantOfType<KtExpression> { it.clear(CodeToInline.PARAMETER_USAGE_KEY) }

                return Argument(defaultValueCopy, null/*TODO*/, isDefaultValue = true)
            }

            is VarargValueArgument -> {
                val arguments = resolvedArgument.arguments
                val single = arguments.singleOrNull()
                if (single?.getSpreadElement() != null) {
                    val expression = single.getArgumentExpression()!!.marked(USER_CODE_KEY)
                    return Argument(expression, bindingContext.getType(expression), isNamed = single.isNamed())
                }

                val elementType = parameter.varargElementType!!
                val expression = psiFactory.buildExpression {
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

    private fun postProcessInsertedCode(range: PsiChildRange, lexicalScope: LexicalScope?): PsiChildRange {
        val elements = range.filterIsInstance<KtElement>().toList()
        if (elements.isEmpty()) return PsiChildRange.EMPTY

        lexicalScope?.let {
            renameDuplicates(elements.dropLast(1).filterIsInstance<KtNamedDeclaration>(), it)
        }

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
                it.clear(CodeToInline.PARAMETER_USAGE_KEY)
                it.clear(CodeToInline.TYPE_PARAMETER_USAGE_KEY)
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
                if (CompileTimeConstantUtils.isArrayFunctionCall(resolvedCall)) {
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

    private operator fun <T: Any> PsiElement.get(key: Key<T>): T? = getCopyableUserData(key)
    private operator fun PsiElement.get(key: Key<Unit>): Boolean = getCopyableUserData(key) != null
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

    // these keys are used on KtValueArgument
    private val MAKE_ARGUMENT_NAMED_KEY = Key<Unit>("MAKE_ARGUMENT_NAMED")
    private val DEFAULT_PARAMETER_VALUE_KEY = Key<Unit>("DEFAULT_PARAMETER_VALUE")
}
