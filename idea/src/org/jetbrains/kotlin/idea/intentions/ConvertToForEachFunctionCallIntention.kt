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
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class ConvertToForEachFunctionCallIntention : SelfTargetingIntention<KtForExpression>(KtForExpression::class.java, "Replace with a 'forEach' function call") {
    override fun isApplicableTo(element: KtForExpression, caretOffset: Int): Boolean {
        val rParen = element.rightParenthesis ?: return false
        if (caretOffset > rParen.endOffset) return false // available only on the loop header, not in the body
        return element.loopRange != null && element.loopParameter != null && element.body != null
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element)

        val body = element.body!!
        val loopParameter = element.loopParameter!!

        val functionBodyArgument: Any = if (body is KtBlockExpression) body.contentRange() else body

        val foreachExpression = KtPsiFactory(element).createExpressionByPattern(
                "$0.forEach{$1->$2}", element.loopRange!!, loopParameter, functionBodyArgument)
        val result = element.replace(foreachExpression)

        commentSaver.restore(result)
    }
}
