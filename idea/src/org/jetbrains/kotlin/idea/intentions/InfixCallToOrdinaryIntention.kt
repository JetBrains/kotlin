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
import org.jetbrains.kotlin.psi.*

public class InfixCallToOrdinaryIntention : JetSelfTargetingIntention<JetBinaryExpression>(javaClass(), "Replace infix call with ordinary call") {
    override fun isApplicableTo(element: JetBinaryExpression, caretOffset: Int): Boolean {
        if (element.getOperationToken() != JetTokens.IDENTIFIER || element.getLeft() == null || element.getRight() == null) return false
        return element.getOperationReference().getTextRange().containsOffset(caretOffset)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val argument = JetPsiUtil.safeDeparenthesize(element.getRight()!!)
        val pattern = "$0.$1" + when (argument) {
            is JetFunctionLiteralExpression -> " $2={}$"
            else -> "($2)"
        }
        val replacement = JetPsiFactory(element).createExpressionByPattern(pattern, element.getLeft()!!, element.getOperationReference().getText(), argument)
        element.replace(replacement)
    }
}
