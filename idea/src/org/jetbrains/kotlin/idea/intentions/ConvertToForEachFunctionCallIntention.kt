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
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset

public class ConvertToForEachFunctionCallIntention : JetSelfTargetingIntention<JetForExpression>(javaClass(), "Replace with a forEach function call") {
    override fun isApplicableTo(element: JetForExpression, caretOffset: Int): Boolean {
        val rParen = element.getRightParenthesis() ?: return false
        if (caretOffset > rParen.endOffset) return false // available only on the loop header, not in the body
        return element.getLoopRange() != null && element.getLoopParameter() != null && element.getBody() != null
    }

    override fun applyTo(element: JetForExpression, editor: Editor) {
        val body = element.getBody()!!
        val loopParameter = element.getLoopParameter()!!

        val functionBodyText = when (body) {
            is JetBlockExpression -> body.getStatements().map { it.getText() }.joinToString("\n")
            else -> body.getText()
        }

        val foreachExpression = JetPsiFactory(element).createExpressionByPattern(
                "$0.forEach{$1->$2}", element.getLoopRange()!!, loopParameter, functionBodyText)
        element.replace(foreachExpression)
    }
}
