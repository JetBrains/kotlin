package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetStringTemplateExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetStringTemplateEntry
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetStringTemplateEntryWithExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression

public class ConvertToConcatenatedStringIntention : JetSelfTargetingIntention<JetStringTemplateExpression>("convert.to.concatenated.string.intention", javaClass()) {
    override fun isApplicableTo(element: JetStringTemplateExpression): Boolean {
        return element.getEntries().any { it is JetStringTemplateEntryWithExpression }
    }

    override fun applyTo(element: JetStringTemplateExpression, editor: Editor) {
        val tripleQuoted = isTripleQuoted(element.getText()!!)
        val quote = if (tripleQuoted) "\"\"\"" else "\""
        val entries = element.getEntries()

        val result = entries.stream()
                .withIndices()
                .map { indexToEntry ->
                    val (index, entry) = indexToEntry
                    entry.toSeparateString(quote, convertExplicitly = index == 0, isFinalEntry = index == entries.size - 1)
                }
                .makeString(separator = "+")
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
        val context = AnalyzerFacadeWithCache.getContextForElement(this)
        val elementType = BindingContextUtils.getRecordedTypeInfo(this, context)?.getType()

        return KotlinBuiltIns.getInstance().isString(elementType)
    }
}
