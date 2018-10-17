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

package org.jetbrains.kotlin.idea.codeInsight.postfix

import com.intellij.codeInsight.template.postfix.templates.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil.findElementOfClassAtRange
import com.intellij.util.Function
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isBoolean


class KtPostfixTemplateProvider : PostfixTemplateProvider {
    override fun getTemplates() = setOf(
        KtNotPostfixTemplate,
        KtIfExpressionPostfixTemplate,
        KtElseExpressionPostfixTemplate,
        KtNotNullPostfixTemplate("notnull"),
        KtNotNullPostfixTemplate("nn"),
        KtIsNullPostfixTemplate,
        KtWhenExpressionPostfixTemplate,
        KtTryPostfixTemplate,
        KtIntroduceVariablePostfixTemplate("val"),
        KtIntroduceVariablePostfixTemplate("var"),
        KtForEachPostfixTemplate("for"),
        KtForEachPostfixTemplate("iter"),
        KtAssertPostfixTemplate,
        KtParenthesizedPostfixTemplate,
        KtSoutPostfixTemplate,
        KtReturnPostfixTemplate,
        KtWhilePostfixTemplate,
        KtWrapWithListOfPostfixTemplate,
        KtWrapWithSetOfPostfixTemplate,
        KtWrapWithArrayOfPostfixTemplate,
        KtWrapWithSequenceOfPostfixTemplate,
        KtSpreadPostfixTemplate
    )

    override fun isTerminalSymbol(currentChar: Char) = currentChar == '.' || currentChar == '!'

    override fun afterExpand(file: PsiFile, editor: Editor) {
    }

    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int) = copyFile

    override fun preExpand(file: PsiFile, editor: Editor) {
    }

    companion object {
        /**
         * In tests only one expression should be suggested, so in case there are many of them, save relevant items
         */
        @TestOnly
        @Volatile
        var previouslySuggestedExpressions = emptyList<String>()
    }
}

private object KtNotPostfixTemplate : NotPostfixTemplate(
    KtPostfixTemplatePsiInfo,
    createExpressionSelector { it.isBoolean() }
)

private class KtIntroduceVariablePostfixTemplate(
    val kind: String
) : PostfixTemplateWithExpressionSelector(kind, "$kind name = expression", createExpressionSelector()) {
    override fun expandForChooseExpression(expression: PsiElement, editor: Editor) {
        KotlinIntroduceVariableHandler.doRefactoring(
            expression.project, editor, expression as KtExpression,
            isVar = kind == "var",
            occurrencesToReplace = null,
            onNonInteractiveFinish = null
        )
    }
}

internal object KtPostfixTemplatePsiInfo : PostfixTemplatePsiInfo() {
    override fun createExpression(context: PsiElement, prefix: String, suffix: String) =
        KtPsiFactory(context.project).createExpression(prefix + context.text + suffix)

    override fun getNegatedExpression(element: PsiElement) = (element as KtExpression).negate()
}

internal fun createExpressionSelector(
    checkCanBeUsedAsValue: Boolean = true,
    statementsOnly: Boolean = false,
    typePredicate: ((KotlinType) -> Boolean)? = null
): PostfixTemplateExpressionSelector {
    val predicate: ((KtExpression, BindingContext) -> Boolean)? =
        if (typePredicate != null) { expression, bindingContext ->
            expression.getType(bindingContext)?.let(typePredicate) ?: false
        }
        else null
    return createExpressionSelectorWithComplexFilter(checkCanBeUsedAsValue, statementsOnly, predicate)
}

internal fun createExpressionSelectorWithComplexFilter(
    // Do not suggest expressions like 'val a = 1'/'for ...'
    checkCanBeUsedAsValue: Boolean = true,
    statementsOnly: Boolean = false,
    predicate: ((KtExpression, BindingContext) -> Boolean)? = null
): PostfixTemplateExpressionSelector = KtExpressionPostfixTemplateSelector(checkCanBeUsedAsValue, statementsOnly, predicate)

private class KtExpressionPostfixTemplateSelector(
    private val checkCanBeUsedAsValue: Boolean,
    private val statementsOnly: Boolean,
    private val predicate: ((KtExpression, BindingContext) -> Boolean)?
) : PostfixTemplateExpressionSelector {

    private fun filterElement(element: PsiElement): Boolean {
        if (element !is KtExpression) return false

        // Can't be independent expressions
        if (element.isSelector || element.parent is KtUserType || element.isOperationReference || element is KtBlockExpression) return false

        // Both KtLambdaExpression and KtFunctionLiteral have the same offset, so we add only one of them -> KtLambdaExpression
        if (element is KtFunctionLiteral) return false

        val bindingContext by lazy { element.analyze(BodyResolveMode.PARTIAL) }

        if (statementsOnly) {
            // We use getQualifiedExpressionForReceiverOrThis because when postfix completion is run on some statement like:
            // foo().try<caret>
            // `element` points to `foo()` call, while we need to select the whole statement with `try` selector
            // to check if it's in a statement position
            if (!KtPsiUtil.isStatement(element.getQualifiedExpressionForReceiverOrThis())) return false
        }
        if (checkCanBeUsedAsValue && !element.canBeUsedAsValue()) return false

        return predicate?.invoke(element, bindingContext) ?: true
    }

    private fun KtExpression.canBeUsedAsValue() =
        !KtPsiUtil.isAssignment(this) &&
                !this.isNamedDeclaration &&
                this !is KtLoopExpression &&
                // if's only with else may be treated as expressions
                !isIfWithoutElse

    private val KtExpression.isIfWithoutElse: Boolean
        get() = (this is KtIfExpression && this.elseKeyword == null)

    override fun getExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
        val originalFile = context.containingFile.originalFile
        val textRange = context.textRange
        val originalElement = findElementOfClassAtRange(originalFile, textRange.startOffset, textRange.endOffset, context::class.java)
            ?: return emptyList()

        val expressions = originalElement.parentsWithSelf
            .filterIsInstance<KtExpression>()
            .takeWhile { !it.isBlockBodyInDeclaration }

        val boundExpression = expressions.firstOrNull { it.parent.endOffset > offset }
        val boundElementParent = boundExpression?.parent
        val filteredByOffset = expressions.takeWhile { it != boundElementParent }.toMutableList()
        if (boundElementParent is KtDotQualifiedExpression && boundExpression == boundElementParent.receiverExpression) {
            val qualifiedExpressionEnd = boundElementParent.endOffset
            expressions
                .dropWhile { it != boundElementParent }
                .drop(1)
                .takeWhile { it.endOffset == qualifiedExpressionEnd }
                .toCollection(filteredByOffset)
        }

        val result = filteredByOffset.filter(this::filterElement)

        if (ApplicationManager.getApplication().isUnitTestMode && result.size > 1) {
            KtPostfixTemplateProvider.previouslySuggestedExpressions = result.map { it.text }
        }

        return result
    }

    override fun hasExpression(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean {
        return !getExpressions(context, copyDocument, newOffset).isEmpty()
    }

    override fun getRenderer() = Function(PsiElement::getText)
}

private val KtExpression.isOperationReference: Boolean
    get() = this.node.elementType == KtNodeTypes.OPERATION_REFERENCE

private val KtElement.isBlockBodyInDeclaration: Boolean
    get() = this is KtBlockExpression && (parent as? KtElement)?.isNamedDeclarationWithBody == true

private val KtElement.isNamedDeclaration: Boolean
    get() = this is KtNamedDeclaration && !isAnonymousFunction

private val KtElement.isNamedDeclarationWithBody: Boolean
    get() = this is KtDeclarationWithBody && !isAnonymousFunction

private val KtDeclaration.isAnonymousFunction: Boolean
    get() = this is KtFunctionLiteral || (this is KtNamedFunction && this.name == null)

private val KtExpression.isSelector: Boolean
    get() = parent is KtQualifiedExpression && (parent as KtQualifiedExpression).selectorExpression == this
