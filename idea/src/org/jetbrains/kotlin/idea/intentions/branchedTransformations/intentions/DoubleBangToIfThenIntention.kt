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
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import org.apache.commons.lang.StringEscapeUtils.escapeJava
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.JetTypeLookupExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPostfixExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetThrowExpression

public class DoubleBangToIfThenIntention : JetSelfTargetingOffsetIndependentIntention<JetPostfixExpression>("double.bang.to.if.then", javaClass()) {

    override fun isApplicableTo(element: JetPostfixExpression): Boolean =
            element.getOperationToken() == JetTokens.EXCLEXCL

    override fun applyTo(element: JetPostfixExpression, editor: Editor) {
        val base = checkNotNull(JetPsiUtil.deparenthesize(element.getBaseExpression()), "Base expression cannot be null")
        val expressionText = formatForUseInExceptionArgument(base.getText()!!)

        val defaultException = JetPsiFactory(element).createExpression("throw $NULL_PTR_EXCEPTION()")

        val isStatement = element.isStatement()
        val isStable = base.isStableVariable()

        val ifStatement = if (isStatement)
            element.convertToIfNullExpression(base, defaultException)
        else
            element.convertToIfNotNullExpression(base, base, defaultException)

        val thrownExpression =
                ((if (isStatement) ifStatement.getThen() else ifStatement.getElse()) as JetThrowExpression).getThrownExpression()!!

        val nullPtrExceptionText = "$NULL_PTR_EXCEPTION(\"%s\")".format(escapeJava(JetBundle.message("double.bang.to.if.then.exception.text", expressionText)))
        val kotlinNullPtrExceptionText = "$KOTLIN_NULL_PTR_EXCEPTION()"

        val exceptionLookupExpression =
                object: JetTypeLookupExpression<String>(listOf(nullPtrExceptionText, kotlinNullPtrExceptionText),
                                                        nullPtrExceptionText, JetBundle.message("double.bang.to.if.then.choose.exception")) {

                    override fun getLookupString(element: String) = element
                    override fun getResult(element: String) = element
                }

        val project = element.getProject()
        val manager = TemplateManagerImpl(project)
        val builder = TemplateBuilderImpl(thrownExpression)
        builder.replaceElement(thrownExpression, exceptionLookupExpression);

        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        editor.getCaretModel().moveToOffset(thrownExpression.getNode()!!.getStartOffset());

        manager.startTemplate(editor, builder.buildInlineTemplate()!!, object: TemplateEditingAdapter() {
            override fun templateFinished(template: Template?, brokenOff: Boolean) {
                if (!isStable && !isStatement) {
                    ifStatement.introduceValueForCondition(ifStatement.getThen()!!, editor)
                }
            }
        })
    }

    fun formatForUseInExceptionArgument(expressionText: String): String {
        val lines = expressionText.split('\n')

        return if (lines.size > 1)
            lines.first().trim() + " ..."
        else
            expressionText.trim()
    }
}
