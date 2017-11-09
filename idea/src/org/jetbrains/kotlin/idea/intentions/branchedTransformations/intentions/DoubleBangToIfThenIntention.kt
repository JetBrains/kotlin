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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.apache.commons.lang.StringEscapeUtils.escapeJava
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.ChooseStringExpression
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStable
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

class DoubleBangToIfThenIntention : SelfTargetingRangeIntention<KtPostfixExpression>(KtPostfixExpression::class.java, "Replace '!!' expression with 'if' expression"), LowPriorityAction {
    override fun applicabilityRange(element: KtPostfixExpression): TextRange? {
        return if (element.operationToken == KtTokens.EXCLEXCL && element.baseExpression != null)
            element.operationReference.textRange
        else
            null
    }

    override fun applyTo(element: KtPostfixExpression, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")

        val base = KtPsiUtil.safeDeparenthesize(element.baseExpression!!, true)
        val expressionText = formatForUseInExceptionArgument(base.text!!)

        val defaultException = KtPsiFactory(element).createExpression("throw NullPointerException()")

        val isStatement = element.isUsedAsStatement(element.analyze())
        val isStable = base.isStable()

        val ifStatement = if (isStatement)
            element.convertToIfNullExpression(base, defaultException)
        else
            element.convertToIfNotNullExpression(base, base, defaultException)

        val thrownExpression =
                ((if (isStatement) ifStatement.then else ifStatement.`else`) as KtThrowExpression).thrownExpression!!

        val message = escapeJava("Expression '$expressionText' must not be null")
        val nullPtrExceptionText = "NullPointerException(\"$message\")"
        val kotlinNullPtrExceptionText = "KotlinNullPointerException()"

        val exceptionLookupExpression = ChooseStringExpression(listOf(nullPtrExceptionText, kotlinNullPtrExceptionText))
        val project = element.project
        val builder = TemplateBuilderImpl(thrownExpression)
        builder.replaceElement(thrownExpression, exceptionLookupExpression)

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        editor.caretModel.moveToOffset(thrownExpression.node!!.startOffset)

        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), object: TemplateEditingAdapter() {
            override fun templateFinished(template: Template?, brokenOff: Boolean) {
                if (!isStable && !isStatement) {
                    ifStatement.introduceValueForCondition(ifStatement.then!!, editor)
                }
            }
        })
    }

    private fun formatForUseInExceptionArgument(expressionText: String): String {
        val lines = expressionText.split('\n')
        return if (lines.size > 1)
            lines.first().trim() + " ..."
        else
            expressionText.trim()
    }
}
