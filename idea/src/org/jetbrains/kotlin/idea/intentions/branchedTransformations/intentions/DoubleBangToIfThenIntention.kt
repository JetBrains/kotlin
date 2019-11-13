/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.ChooseStringExpression
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableSimpleExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

class DoubleBangToIfThenIntention :
    SelfTargetingRangeIntention<KtPostfixExpression>(KtPostfixExpression::class.java, "Replace '!!' expression with 'if' expression"),
    LowPriorityAction {
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
        val isStable = base.isStableSimpleExpression()

        val ifStatement = if (isStatement)
            element.convertToIfNullExpression(base, defaultException)
        else {
            val qualifiedExpressionForReceiver = element.getQualifiedExpressionForReceiver()
            val selectorExpression = qualifiedExpressionForReceiver?.selectorExpression
            val thenClause = selectorExpression?.let {
                KtPsiFactory(element).createExpressionByPattern("$0.$1", base, it)
            } ?: base
            (qualifiedExpressionForReceiver ?: element).convertToIfNotNullExpression(base, thenClause, defaultException)
        }

        val thrownExpression =
            ((if (isStatement) ifStatement.then else ifStatement.`else`) as KtThrowExpression).thrownExpression!!
        val message = StringUtil.escapeStringCharacters("Expression '$expressionText' must not be null")
        val nullPtrExceptionText = "NullPointerException(\"$message\")"
        val kotlinNullPtrExceptionText = "KotlinNullPointerException()"

        val exceptionLookupExpression = ChooseStringExpression(listOf(nullPtrExceptionText, kotlinNullPtrExceptionText))
        val project = element.project
        val builder = TemplateBuilderImpl(thrownExpression)
        builder.replaceElement(thrownExpression, exceptionLookupExpression)

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        editor.caretModel.moveToOffset(thrownExpression.node!!.startOffset)

        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), object : TemplateEditingAdapter() {
            override fun templateFinished(template: Template, brokenOff: Boolean) {
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
