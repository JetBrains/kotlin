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

public class ConvertToConcatenatedStringIntention : JetSelfTargetingIntention<JetStringTemplateExpression>("convert.to.concatenated.string.intention", javaClass()) {
    override fun isApplicableTo(element: JetStringTemplateExpression): Boolean {
        return !element.getEntries().isEmpty() && element.getEntries().any({ t -> t is JetStringTemplateEntryWithExpression })
    }

    override fun applyTo(element: JetStringTemplateExpression, editor: Editor) {
        val tripleQuoted = isTripleQuoted(element.getText()!!)
        val quote = if (tripleQuoted) "\"\"\"" else "\""

        val (needsClosingQuote, res) = element.getEntries().fold(Pair(false, ""), { x, t -> folder(x, t, quote) })
        val result = if (needsClosingQuote) res + quote else res
        val replacement = JetPsiFactory.createExpression(element.getProject(), result)
        element.replace(replacement)
    }

    private fun folder(accn: Pair<Boolean, String>, element: JetStringTemplateEntry, quote: String): Pair<Boolean, String> {
        val (afterStringLiteral, acc) = accn

        val (isStringLiteral, elementText) = when (element) {
            is JetStringTemplateEntryWithExpression -> {
                val expr = element.getExpression()!!
                val str = stringValueFor(expr, acc.isEmpty())
                Pair(false, str)
            }
            else -> Pair(true, element.getText()!!)
        }

        val str = when {
            acc.isEmpty() && isStringLiteral -> quote + elementText
            acc.isEmpty() && !isStringLiteral -> elementText
            isStringLiteral && afterStringLiteral -> acc + elementText
            isStringLiteral && !afterStringLiteral -> "$acc + $quote$elementText"
            !isStringLiteral && afterStringLiteral -> "$acc$quote + $elementText"
            !isStringLiteral && !afterStringLiteral -> "$acc + $elementText"
            else -> throw java.lang.IllegalStateException()
        }
        return Pair(isStringLiteral, str)
    }

    private fun isTripleQuoted(str: String) = str.startsWith("\"\"\"") && str.endsWith("\"\"\"")

    private fun stringValueFor(expression: JetExpression, mustConvertExplicitly: Boolean): String {
        val expressionText = if (expression is JetBinaryExpression) "(${expression.getText()})" else expression.getText()!!
        if (!mustConvertExplicitly) return expressionText

        val context = AnalyzerFacadeWithCache.getContextForElement(expression)
        val elementType = BindingContextUtils.getRecordedTypeInfo(expression, context)?.getType()

        if (KotlinBuiltIns.getInstance().isString(elementType)) {
            return expressionText
        }
        else {
            return expressionText + ".toString()"
        }

    }

}
