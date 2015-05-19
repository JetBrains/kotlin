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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.core.refactoring.JetNameValidator
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectElementsOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import java.util.ArrayList

//TODO: replacement of class usages
//TODO: different replacements for property accessors
//TODO: replace all in project quickfixes on usage and on deprecated annotation
public class DeprecatedSymbolUsageFix(
        element: JetSimpleNameExpression/*TODO?*/,
        val replaceWith: ReplaceWith
) : JetIntentionAction<JetSimpleNameExpression>(element) {

    override fun getFamilyName() = "Replace deprecated symbol usage"

    override fun getText() = "Replace with '${replaceWith.expression}'" //TODO: substitute?

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

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val psiFactory = JetPsiFactory(project)
        val bindingContext = element.analyze()
        val resolvedCall = element.getResolvedCall(bindingContext)!!
        val descriptor = resolvedCall.getResultingDescriptor()
        val callExpression = resolvedCall.getCall().getCallElement() as JetExpression
        val qualifiedExpression = callExpression.getParent() as? JetQualifiedExpression
        val expressionToReplace = qualifiedExpression ?: callExpression

        var receiver = element.getReceiverExpression()
        receiver?.putCopyableUserData(USER_CODE_KEY, Unit)

        if (receiver == null) {
            val receiverValue = if (descriptor.isExtension) resolvedCall.getExtensionReceiver() else resolvedCall.getDispatchReceiver()
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expressionToReplace]
            if (receiverValue is ThisReceiver && resolutionScope != null) {
                receiver = receiverValue.asExpression(resolutionScope, psiFactory)
            }
        }

        receiver?.putCopyableUserData(FROM_THIS_KEY, Unit)

        val originalDescriptor = (if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor).getOriginal()

        var (expression, imports, parameterUsages) = ReplaceWithAnnotationAnalyzer.analyze(
                replaceWith, originalDescriptor, element.getResolutionFacade(), file, project)

        //TODO: this@
        for (thisExpression in expression.collectElementsOfType<JetThisExpression>()) {
            if (receiver != null) {
                thisExpression.replace(receiver)
            }
            else {
                thisExpression.putCopyableUserData(FROM_THIS_KEY, Unit)
            }
        }

        fun argumentForParameter(parameter: ValueParameterDescriptor): JetExpression? {
            //TODO: optional parameters
            val arguments = resolvedCall.getValueArguments()[parameter] ?: return null //TODO: what if not? vararg?
            return arguments.getArguments().firstOrNull()?.getArgumentExpression() //TODO: what if multiple?
        }

        val introduceValuesForParameters = ArrayList<Pair<ValueParameterDescriptor, JetExpression>>()

        //TODO: check for dropping expressions with potential side effects
        for (parameter in descriptor.getValueParameters()) {
            val argument = argumentForParameter(parameter) ?: continue
            argument.putCopyableUserData(FROM_PARAMETER_KEY, parameter)
            argument.putCopyableUserData(USER_CODE_KEY, Unit)

            val usages = parameterUsages[parameter.getOriginal()]!!
            usages.forEach { it.replace(argument) }

            if (usages.size() > 1 && argument.shouldIntroduceVariableIfUsedTwice()) {
                introduceValuesForParameters.add(parameter to argument)
            }
        }

        if (qualifiedExpression is JetSafeQualifiedExpression) {
            expression = expression.wrapExpressionForSafeCall(expressionToReplace, receiver!!, bindingContext)
        }
        else if (callExpression is JetBinaryExpression && callExpression.getOperationToken() == JetTokens.IDENTIFIER) {
            expression = expression.keepInfixFormIfPossible()
        }

        if (receiver != null && receiver.shouldIntroduceVariableIfUsedTwice()) {
            val thisReplaced = expression.collectElementsOfType<JetExpression> { it.getCopyableUserData(FROM_THIS_KEY) != null }
            if (thisReplaced.size() > 1) {
                expression = expression.introduceValue(receiver, expressionToReplace, bindingContext, thisReplaced)
            }
        }

        for ((parameter, value) in introduceValuesForParameters) {
            val usagesReplaced = expression.collectElementsOfType<JetExpression> { it.getCopyableUserData(FROM_PARAMETER_KEY) == parameter }
            assert(usagesReplaced.size() > 1)
            expression = expression.introduceValue(value, expressionToReplace, bindingContext, usagesReplaced, nameSuggestion = parameter.getName().asString())
        }

        var result = expressionToReplace.replaced(expression)

        //TODO: drop import of old function (if not needed anymore)?

        for (importFqName in imports) {
            val descriptors = file.getResolutionFacade().resolveImportReference(file, importFqName)
            val descriptorToImport = descriptors.firstOrNull() ?: continue
            ImportInsertHelper.getInstance(project).importDescriptor(file, descriptorToImport)
        }

        val shortenFilter = { element: PsiElement ->
            if (element.getCopyableUserData(USER_CODE_KEY) != null) {
                ShortenReferences.FilterResult.SKIP
            }
            else {
                val thisReceiver = (element as? JetQualifiedExpression)?.getReceiverExpression() as? JetThisExpression
                if (thisReceiver != null && thisReceiver.getCopyableUserData(USER_CODE_KEY) != null) // don't remove explicit 'this' coming from user's code
                    ShortenReferences.FilterResult.GO_INSIDE
                else
                    ShortenReferences.FilterResult.PROCESS
            }
        }
        result = ShortenReferences({ ShortenReferences.Options(removeThis = true) }).process(result, shortenFilter) as JetExpression

        // clean up user data
        result.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.putCopyableUserData(USER_CODE_KEY, null)
                element.putCopyableUserData(FROM_PARAMETER_KEY, null)
                element.putCopyableUserData(FROM_THIS_KEY, null)
            }
        })

        val offset = (result.getCalleeExpressionIfAny() ?: result).getTextOffset()
        editor?.moveCaret(offset)
    }

    private fun JetExpression.wrapExpressionForSafeCall(
            expressionToReplace: JetExpression,
            receiver: JetExpression,
            bindingContext: BindingContext
    ): JetExpression {
        val psiFactory = JetPsiFactory(this)
        val qualified = this as? JetQualifiedExpression
        if (qualified != null) {
            if (qualified.getReceiverExpression().getCopyableUserData(FROM_THIS_KEY) != null) {
                if (qualified is JetSafeQualifiedExpression) return this // already safe
                val selector = qualified.getSelectorExpression()
                if (selector != null) {
                    return psiFactory.createExpressionByPattern("$0?.$1", receiver, selector)
                }
            }
        }

        if (expressionToReplace.isUsedAsExpression(bindingContext)) {
            val thisReplaced = this.collectElementsOfType<JetExpression> { it.getCopyableUserData(FROM_THIS_KEY) != null }
            return this.introduceValue(receiver, expressionToReplace, bindingContext, thisReplaced, safeCall = true)
        }
        else {
            return psiFactory.createExpressionByPattern("if ($0 != null) { $1 }", receiver, this)
        }
    }

    private fun JetExpression.keepInfixFormIfPossible(): JetExpression {
        if (this !is JetDotQualifiedExpression) return this
        val receiver = getReceiverExpression()
        if (receiver.getCopyableUserData(FROM_THIS_KEY) == null) return this
        val call = getSelectorExpression() as? JetCallExpression ?: return this
        val nameExpression = call.getCalleeExpression() as? JetSimpleNameExpression ?: return this
        val argument = call.getValueArguments().singleOrNull() ?: return this
        if (argument.getArgumentName() != null) return this
        val argumentExpression = argument.getArgumentExpression() ?: return this
        return JetPsiFactory(this).createExpressionByPattern("$0 ${nameExpression.getText()} $1", receiver, argumentExpression)
    }

    private fun JetExpression.introduceValue(
            value: JetExpression,
            insertDeclarationsBefore: JetExpression,
            bindingContext: BindingContext,
            usages: Collection<JetExpression>,
            nameSuggestion: String? = null,
            safeCall: Boolean = false
    ): JetExpression {
        assert(usages.all { isAncestor(it, strict = true) })

        val psiFactory = JetPsiFactory(this)

        fun nameInCode(name: String) = IdeDescriptorRenderers.SOURCE_CODE.renderName(Name.identifier(name))

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

                val valueType = bindingContext.getType(value)
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
            = scope.collectElementsOfType<JetSimpleNameExpression> { it.getReceiverExpression() == null && it.getReferencedName() == name }

    private fun JetExpression?.shouldIntroduceVariableIfUsedTwice(): Boolean {
        return when (this) {
            is JetSimpleNameExpression -> false
            is JetQualifiedExpression -> getReceiverExpression().shouldIntroduceVariableIfUsedTwice() || getSelectorExpression().shouldIntroduceVariableIfUsedTwice()
            is JetUnaryExpression -> getOperationToken() in setOf(JetTokens.PLUSPLUS, JetTokens.MINUSMINUS) || getBaseExpression().shouldIntroduceVariableIfUsedTwice()
            is JetStringTemplateExpression -> getEntries().any { it is JetStringTemplateEntryWithExpression }
            is JetThisExpression, is JetSuperExpression -> false
            is JetParenthesizedExpression -> getExpression().shouldIntroduceVariableIfUsedTwice()
            is JetBinaryExpression, is JetIfExpression -> true // TODO: discuss it
            else -> true // what else it can be?
        }
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val nameExpression = diagnostic.getPsiElement() as? JetSimpleNameExpression ?: return null
            val descriptor = Errors.DEPRECATED_SYMBOL_WITH_MESSAGE.cast(diagnostic).getA()
            val replacement = replaceWithPattern(descriptor) ?: return null
            return DeprecatedSymbolUsageFix(nameExpression, replacement)
        }

        private fun replaceWithPattern(descriptor: DeclarationDescriptor): ReplaceWith? {
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

        private val USER_CODE_KEY = Key<Unit>("USER_CODE")
        private val FROM_PARAMETER_KEY = Key<ValueParameterDescriptor>("FROM_PARAMETER")
        private val FROM_THIS_KEY = Key<Unit>("FROM_THIS")
    }
}
