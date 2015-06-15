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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContextUtils

public class ConvertToConcatenatedStringIntention : JetSelfTargetingOffsetIndependentIntention<JetStringTemplateExpression>(javaClass(), "Convert template to concatenated string"), LowPriorityAction {
    override fun isApplicableTo(element: JetStringTemplateExpression): Boolean {
        if (element.getLastChild().getNode().getElementType() != JetTokens.CLOSING_QUOTE) return false // not available for unclosed literal
        return element.getEntries().any { it is JetStringTemplateEntryWithExpression }
    }

    override fun applyTo(element: JetStringTemplateExpression, editor: Editor) {
        val tripleQuoted = isTripleQuoted(element.getText()!!)
        val quote = if (tripleQuoted) "\"\"\"" else "\""
        val entries = element.getEntries()

        val text = entries
                .filterNot { it is JetStringTemplateEntryWithExpression && it.getExpression() == null }
                .mapIndexed { index, entry ->
                    entry.toSeparateString(quote, convertExplicitly = (index == 0), isFinalEntry = (index == entries.lastIndex))
                }
                .join(separator = "+")
                .replace("""$quote+$quote""", "")

        val replacement = JetPsiFactory(element).createExpression(text)
        element.replace(replacement)
    }

    private fun isTripleQuoted(str: String) = str.startsWith("\"\"\"") && str.endsWith("\"\"\"")

    private fun JetStringTemplateEntry.toSeparateString(quote: String, convertExplicitly: Boolean, isFinalEntry: Boolean): String {
        if (this !is JetStringTemplateEntryWithExpression) {
            return getText().quote(quote)
        }

        val expression = getExpression()!! // checked before

        val text = if (needsParenthesis(expression, isFinalEntry))
            "(" + expression.getText() + ")"
        else
            expression.getText()

        return if (convertExplicitly && !expression.isStringExpression())
            text + ".toString()"
        else
            text
    }

    private fun needsParenthesis(expression: JetExpression, isFinalEntry: Boolean): Boolean {
        return when (expression) {
            is JetBinaryExpression -> true
            is JetIfExpression -> expression.getElse() !is JetBlockExpression && !isFinalEntry
            else -> false
        }
    }

    private fun String.quote(quote: String) = quote + this + quote

    private fun JetExpression.isStringExpression() = KotlinBuiltIns.isString(analyze().getType(this))
}
