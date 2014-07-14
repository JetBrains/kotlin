/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lexer.JetTokens

public class ReplaceWithDotQualifiedMethodCallIntention : JetSelfTargetingIntention<JetBinaryExpression>("replace.with.dot.qualified.method.call.intention", javaClass()) {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        return element.getLeft() != null && element.getRight() != null && element.getOperationToken() == JetTokens.IDENTIFIER
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val receiverText = element.getLeft()!!.getText()
        val argumentText = element.getRight()!!.getText()
        val functionName = element.getOperationReference().getText()
        val replacementExpressionStringBuilder = StringBuilder("$receiverText.$functionName")

        replacementExpressionStringBuilder.append(
                when (element.getRight()) {
                    is JetFunctionLiteralExpression -> " $argumentText"
                    is JetParenthesizedExpression -> argumentText
                    else -> "($argumentText)"
                }
        )

        val replacement = JetPsiFactory(element).createExpression(replacementExpressionStringBuilder.toString())
        element.replace(replacement)
    }
}