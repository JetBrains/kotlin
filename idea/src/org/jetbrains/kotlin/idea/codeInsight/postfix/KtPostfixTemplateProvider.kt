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
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
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
            KtWhilePostfixTemplate
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
        // Do not suggest expressions like 'val a = 1'/'for ...'
        checkCanBeUsedAsValue: Boolean = true,
        statementsOnly: Boolean = false,
        typePredicate: ((KotlinType) -> Boolean)? = null
): PostfixTemplateExpressionSelectorBase = KtExpressionPostfixTemplateSelector(statementsOnly, checkCanBeUsedAsValue, typePredicate)

private class KtExpressionPostfixTemplateSelector(
        statementsOnly: Boolean,
        checkCanBeUsedAsValue: Boolean,
        typePredicate: ((KotlinType) -> Boolean)?
) : PostfixTemplateExpressionSelectorBase(createFilter(statementsOnly, checkCanBeUsedAsValue, typePredicate)) {
    companion object {
        private fun createFilter(
                statementsOnly: Boolean,
                checkCanBeUsedAsValue: Boolean,
                typePredicate: ((KotlinType) -> Boolean)?
        ): Condition<PsiElement> = Condition {
            if (it !is KtExpression) return@Condition false

            // Can't be independent expressions
            if (it.isSelector || it.parent is KtUserType || it.isOperationReference || it is KtBlockExpression) return@Condition false

            // Both KtLambdaExpression and KtFunctionLiteral have the same offset, so we add only one of them -> KtLambdaExpression
            if (it is KtFunctionLiteral) return@Condition false

            val context by lazy { it.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION) }

            if (statementsOnly && it.parent !is KtBlockExpression && !it.isUsedAsStatement(context)) return@Condition false
            if (checkCanBeUsedAsValue && !it.canBeUsedAsValue()) return@Condition false

            typePredicate == null || it.getType(context)?.let { typePredicate(it) } ?: false
        }

        private fun KtExpression.canBeUsedAsValue() =
                !KtPsiUtil.isAssignment(this) &&
                !this.isNamedDeclaration &&
                this !is KtLoopExpression &&
                // if's only with else may be treated as expressions
                !isIfWithoutElse

        private val KtExpression.isIfWithoutElse: Boolean
            get() = (this is KtIfExpression && this.elseKeyword == null)
    }

    override fun getExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
        val expressions = super.getExpressions(context, document, offset)

        if (ApplicationManager.getApplication().isUnitTestMode && expressions.size > 1) {
            KtPostfixTemplateProvider.previouslySuggestedExpressions = expressions.map { it.text }
        }

        return expressions
    }

    override fun getNonFilteredExpressions(
            context: PsiElement,
            document: Document,
            offset: Int
    ) = context.parentsWithSelf
            .filterIsInstance<KtExpression>()
            .takeWhile {
                !it.isBlockBodyInDeclaration
            }.toList()
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
