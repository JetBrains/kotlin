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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class ConvertToConcatenatedStringIntention : SelfTargetingOffsetIndependentIntention<KtStringTemplateExpression>(KtStringTemplateExpression::class.java, "Convert template to concatenated string"), LowPriorityAction {
    override fun isApplicableTo(element: KtStringTemplateExpression): Boolean {
        if (element.lastChild.node.elementType != KtTokens.CLOSING_QUOTE) return false // not available for unclosed literal
        return element.entries.any { it is KtStringTemplateEntryWithExpression }
    }

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val tripleQuoted = isTripleQuoted(element.text!!)
        val quote = if (tripleQuoted) "\"\"\"" else "\""
        val entries = element.entries

        val text = entries
                .filterNot { it is KtStringTemplateEntryWithExpression && it.expression == null }
                .mapIndexed { index, entry ->
                    entry.toSeparateString(quote, convertExplicitly = (index == 0), isFinalEntry = (index == entries.lastIndex))
                }
                .joinToString(separator = "+")
                .replace("""$quote+$quote""", "")

        val replacement = KtPsiFactory(element).createExpression(text)
        element.replace(replacement)
    }

    private fun isTripleQuoted(str: String) = str.startsWith("\"\"\"") && str.endsWith("\"\"\"")

    private fun KtStringTemplateEntry.toSeparateString(quote: String, convertExplicitly: Boolean, isFinalEntry: Boolean): String {
        if (this !is KtStringTemplateEntryWithExpression) {
            return text.quote(quote)
        }

        val expression = expression!! // checked before

        val text = if (needsParenthesis(expression, isFinalEntry))
            "(" + expression.text + ")"
        else
            expression.text

        return if (convertExplicitly && !expression.isStringExpression())
            text + ".toString()"
        else
            text
    }

    private fun needsParenthesis(expression: KtExpression, isFinalEntry: Boolean): Boolean {
        return when (expression) {
            is KtBinaryExpression -> true
            is KtIfExpression -> expression.`else` !is KtBlockExpression && !isFinalEntry
            else -> false
        }
    }

    private fun String.quote(quote: String) = quote + this + quote

    private fun KtExpression.isStringExpression() = KotlinBuiltIns.isString(analyze().getType(this))
}
