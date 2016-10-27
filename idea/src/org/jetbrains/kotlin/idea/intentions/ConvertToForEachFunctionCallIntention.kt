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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import java.util.*

class ConvertToForEachFunctionCallIntention : SelfTargetingIntention<KtForExpression>(KtForExpression::class.java, "Replace with a 'forEach' function call") {
    override fun isApplicableTo(element: KtForExpression, caretOffset: Int): Boolean {
        val rParen = element.rightParenthesis ?: return false
        if (caretOffset > rParen.endOffset) return false // available only on the loop header, not in the body
        return element.loopRange != null && element.loopParameter != null && element.body != null
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element)

        val labelName = element.getLabelName()

        val body = element.body!!
        val loopParameter = element.loopParameter!!

        val functionBodyArgument: Any = (body as? KtBlockExpression)?.contentRange() ?: body

        val psiFactory = KtPsiFactory(element)
        val foreachExpression = psiFactory.createExpressionByPattern(
                "$0.forEach{$1->$2}", element.loopRange!!, loopParameter, functionBodyArgument)
        val result = element.replace(foreachExpression) as KtElement

        result.findDescendantOfType<KtFunctionLiteral>()!!.getContinuesWithLabel(labelName).forEach {
            it.replace(psiFactory.createExpression("return@forEach"))
        }

        commentSaver.restore(result)
    }

    private fun KtElement.getContinuesWithLabel(labelName: String?): List<KtContinueExpression> {
        val continueElements = ArrayList<KtContinueExpression>()

        forEachDescendantOfType<KtContinueExpression>({ it.shouldEnterForUnqualified(this) }) {
            if (it.getLabelName() == null) {
                continueElements += it
            }
        }

        if (labelName != null) {
            forEachDescendantOfType<KtContinueExpression>({ it.shouldEnterForQualified(this, labelName) }) {
                if (it.getLabelName() == labelName) {
                    continueElements += it
                }
            }
        }

        return continueElements
    }

    private fun PsiElement.shouldEnterForUnqualified(allow: PsiElement): Boolean {
        if (this == allow) return true
        if (shouldNeverEnter()) return false
        return this !is KtLoopExpression
    }

    private fun PsiElement.shouldEnterForQualified(allow: PsiElement, labelName: String): Boolean {
        if (this == allow) return true
        if (shouldNeverEnter()) return false
        return this !is KtLoopExpression || getLabelName() != labelName
    }

    private fun PsiElement.shouldNeverEnter() = this is KtLambdaExpression || this is KtClassOrObject || this is KtFunction

    private fun KtLoopExpression.getLabelName() = (parent as? KtExpressionWithLabel)?.getLabelName()
}
