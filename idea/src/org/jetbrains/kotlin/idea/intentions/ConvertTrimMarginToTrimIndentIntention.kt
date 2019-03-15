/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ConvertTrimMarginToTrimIndentIntention : SelfTargetingIntention<KtCallExpression>(
    KtCallExpression::class.java, "Convert to 'trimIndent'"
) {
    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        val template = (element.getQualifiedExpressionForSelector()?.receiverExpression as? KtStringTemplateExpression) ?: return false
        if (!template.text.startsWith("\"\"\"")) return false

        val callee = element.calleeExpression ?: return false
        if (callee.text != "trimMargin" || callee.getCallableDescriptor()?.fqNameSafe != FqName("kotlin.text.trimMargin")) return false

        val marginPrefix = element.marginPrefix() ?: return false

        val entries = template.entries
        if (!listOfNotNull(entries.firstOrNull(), entries.lastOrNull()).all { it.text.isLineBreakOrBlank() }) {
            return false
        }

        return entries.drop(1).dropLast(1).all { stringTemplateEntry ->
            val text = stringTemplateEntry.text
            text.isLineBreakOrBlank() || text.dropWhile { it.isWhitespace() }.startsWith(marginPrefix)
        }
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val qualifiedExpression = element.getQualifiedExpressionForSelector()
        val template = (qualifiedExpression?.receiverExpression as? KtStringTemplateExpression) ?: return
        val marginPrefix = element.marginPrefix() ?: return

        val indent = template.entries.asSequence().mapNotNull { stringTemplateEntry ->
            val text = stringTemplateEntry.text
            if (text.isLineBreakOrBlank()) null else text.takeWhile { it.isWhitespace() }
        }.minBy { it.length } ?: ""

        val newTemplate = buildString {
            template.entries.forEach { entry ->
                val text = entry.text
                if (text.isLineBreakOrBlank()) {
                    append(text)
                } else {
                    append(indent)
                    append(entry.text.dropWhile { it.isWhitespace() }.replaceFirst(marginPrefix, ""))
                }
            }
        }
        qualifiedExpression.replace(KtPsiFactory(element).createExpression("\"\"\"$newTemplate\"\"\".trimIndent()"))
    }
}

private fun KtCallExpression.marginPrefix(): String? {
    val argument = valueArguments.firstOrNull()?.getArgumentExpression()
    if (argument != null) {
        if (argument !is KtStringTemplateExpression) return null
        val entry = argument.entries.toList().singleOrNull() as? KtLiteralStringTemplateEntry ?: return null
        return entry.text.replace("\"", "")
    }
    return "|"
}

private fun String.isLineBreakOrBlank() = this == "\n" || this.isBlank()