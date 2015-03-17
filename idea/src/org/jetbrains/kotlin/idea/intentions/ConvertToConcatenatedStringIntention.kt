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

import org.jetbrains.kotlin.psi.JetStringTemplateExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetStringTemplateEntry
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public class ConvertToConcatenatedStringIntention : JetSelfTargetingIntention<JetStringTemplateExpression>("convert.to.concatenated.string.intention", javaClass()) {
    override fun isApplicableTo(element: JetStringTemplateExpression): Boolean {
        return element.getEntries().any { it is JetStringTemplateEntryWithExpression }
    }

    override fun applyTo(element: JetStringTemplateExpression, editor: Editor) {
        val tripleQuoted = isTripleQuoted(element.getText()!!)
        val quote = if (tripleQuoted) "\"\"\"" else "\""
        val entries = element.getEntries()

        val result = entries.stream()
                .mapIndexed { index, entry ->
                    entry.toSeparateString(quote, convertExplicitly = index == 0, isFinalEntry = index == entries.size() - 1)
                }
                .join(separator = "+")
                .replaceAll("""$quote\+$quote""", "")

        val replacement = JetPsiFactory(element).createExpression(result)

        element.replace(replacement)
    }

    private fun isTripleQuoted(str: String) = str.startsWith("\"\"\"") && str.endsWith("\"\"\"")

    private fun JetStringTemplateEntry.toSeparateString(quote: String, convertExplicitly: Boolean, isFinalEntry: Boolean): String {
        if (this !is JetStringTemplateEntryWithExpression) return getText()!!.quote(quote)

        val expression = getExpression()!!

        val expressionText = if (needsParenthesis(expression, isFinalEntry)) "(${expression.getText()})" else expression.getText()!!
        return if (convertExplicitly && !expression.isStringExpression()) {
            expressionText + ".toString()"
        }
        else {
            expressionText
        }
    }

    private fun needsParenthesis(expression: JetExpression, isFinalEntry: Boolean) = when (expression) {
        is JetBinaryExpression -> true
        is JetIfExpression -> expression.getElse() !is JetBlockExpression && !isFinalEntry
        else -> false
    }

    private fun String.quote(quote: String) = quote + this + quote

    private fun JetExpression.isStringExpression(): Boolean {
        val context = this.analyze()
        val elementType = BindingContextUtils.getRecordedTypeInfo(this, context)?.getType()

        return KotlinBuiltIns.isString(elementType)
    }
}
