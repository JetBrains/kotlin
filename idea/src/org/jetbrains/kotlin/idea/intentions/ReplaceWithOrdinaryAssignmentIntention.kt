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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern

public class ReplaceWithOrdinaryAssignmentIntention : JetSelfTargetingIntention<JetBinaryExpression>(javaClass(), "Replace with ordinary assignment") {
    override fun isApplicableTo(element: JetBinaryExpression, caretOffset: Int): Boolean {
        if (element.getOperationToken() !in JetTokens.AUGMENTED_ASSIGNMENTS) return false
        if (element.getLeft() !is JetSimpleNameExpression) return false
        if (element.getRight() == null) return false
        return element.getOperationReference().getTextRange().containsOffset(caretOffset)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val left = element.getLeft()!!
        val right = element.getRight()!!
        val factory = JetPsiFactory(element)

        val assignOpText = element.getOperationReference().getText()
        assert(assignOpText.endsWith("="))
        val operationText = assignOpText.substring(0, assignOpText.length() - 1)

        element.replace(factory.createExpressionByPattern("$0 = $0 $operationText $1", left, right))
    }
}
