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

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.apache.commons.lang.StringEscapeUtils.escapeJava
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.JetTypeLookupExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPostfixExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetThrowExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

public class DoubleBangToIfThenIntention : JetSelfTargetingRangeIntention<JetPostfixExpression>(javaClass(), "Replace '!!' expression with 'if' expression") {
    override fun applicabilityRange(element: JetPostfixExpression): TextRange? {
        return if (element.getOperationToken() == JetTokens.EXCLEXCL)  element.getOperationReference().getTextRange() else null
    }

    override fun applyTo(element: JetPostfixExpression, editor: Editor) {
        val base = JetPsiUtil.safeDeparenthesize(element.getBaseExpression())
        val expressionText = formatForUseInExceptionArgument(base.getText()!!)

        val defaultException = JetPsiFactory(element).createExpression("throw NullPointerException()")

        val isStatement = element.isUsedAsStatement(element.analyze())
        val isStable = base.isStableVariable()

        val ifStatement = if (isStatement)
            element.convertToIfNullExpression(base, defaultException)
        else
            element.convertToIfNotNullExpression(base, base, defaultException)

        val thrownExpression =
                ((if (isStatement) ifStatement.getThen() else ifStatement.getElse()) as JetThrowExpression).getThrownExpression()!!

        val message = escapeJava("Expression '$expressionText' must not be null")
        val nullPtrExceptionText = "NullPointerException(\"$message\")"
        val kotlinNullPtrExceptionText = "KotlinNullPointerException()"

        val exceptionLookupExpression =
                object: JetTypeLookupExpression<String>(listOf(nullPtrExceptionText, kotlinNullPtrExceptionText),
                                                        nullPtrExceptionText) {
                    override fun getLookupString(element: String) = element
                    override fun getResult(element: String) = element
                }

        val project = element.getProject()
        val builder = TemplateBuilderImpl(thrownExpression)
        builder.replaceElement(thrownExpression, exceptionLookupExpression);

        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        editor.getCaretModel().moveToOffset(thrownExpression.getNode()!!.getStartOffset());

        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), object: TemplateEditingAdapter() {
            override fun templateFinished(template: Template?, brokenOff: Boolean) {
                if (!isStable && !isStatement) {
                    ifStatement.introduceValueForCondition(ifStatement.getThen()!!, editor)
                }
            }
        })
    }

    private fun formatForUseInExceptionArgument(expressionText: String): String {
        val lines = expressionText.split('\n')
        return if (lines.size() > 1)
            lines.first().trim() + " ..."
        else
            expressionText.trim()
    }
}
