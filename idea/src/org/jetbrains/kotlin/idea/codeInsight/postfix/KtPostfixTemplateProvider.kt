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
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
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
            KtForEachPostfixTemplate("iter")
    )

    override fun isTerminalSymbol(currentChar: Char) = currentChar == '.' || currentChar == '!'

    override fun afterExpand(file: PsiFile, editor: Editor) {
    }

    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int) = copyFile

    override fun preExpand(file: PsiFile, editor: Editor) {
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
        statementsOnly: Boolean = false,
        predicate: ((KotlinType) -> Boolean)? = null
): PostfixTemplateExpressionSelectorBase =
        KtExpressionPostfixTemplateSelector {
            if (this !is KtExpression) return@KtExpressionPostfixTemplateSelector false
            if (statementsOnly && !isStatement()) return@KtExpressionPostfixTemplateSelector false

            if (predicate == null) return@KtExpressionPostfixTemplateSelector true

            getType(analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION))?.let { predicate(it) } ?: false
        }

private fun PsiElement.isStatement() = parent is KtBlockExpression

private class KtExpressionPostfixTemplateSelector(
        val filter: PsiElement.() -> Boolean
) : PostfixTemplateExpressionSelectorBase(Condition(filter)) {
    override fun getNonFilteredExpressions(
            context: PsiElement,
            document: Document,
            offset: Int
    ) = context.parentsWithSelf
            .filterIsInstance<KtExpression>()
            .takeWhile { it !is KtBlockExpression && it !is KtDeclarationWithBody && !it.isEffectivelyDeclaration() }
            .toList()
}

private fun KtElement.isEffectivelyDeclaration() =
        this is KtNamedDeclaration &&
        // function literal is an expression while it's also a subtype of KtNamedDeclaration
        this !is KtFunctionLiteral &&
        // !(fun (a) = 1)
        (this !is KtNamedFunction || this.name == null)
