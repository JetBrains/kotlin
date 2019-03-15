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

class ConvertTrimIndentToTrimMarginIntention : SelfTargetingIntention<KtCallExpression>(
    KtCallExpression::class.java, "Convert to 'trimMargin'"
) {
    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        val template = (element.getQualifiedExpressionForSelector()?.receiverExpression as? KtStringTemplateExpression) ?: return false
        if (!template.text.startsWith("\"\"\"")) return false

        val callee = element.calleeExpression ?: return false
        if (callee.text != "trimIndent" || callee.getCallableDescriptor()?.fqNameSafe != FqName("kotlin.text.trimIndent")) return false

        val entries = template.entries
        return listOfNotNull(entries.firstOrNull(), entries.lastOrNull()).all { it.text.isLineBreakOrBlank() }
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val qualifiedExpression = element.getQualifiedExpressionForSelector()
        val template = (qualifiedExpression?.receiverExpression as? KtStringTemplateExpression) ?: return

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
                    append("|")
                    append(text.drop(indent.length))
                }
            }
        }
        qualifiedExpression.replace(KtPsiFactory(element).createExpression("\"\"\"$newTemplate\"\"\".trimMargin()"))
    }
}

private fun String.isLineBreakOrBlank() = this == "\n" || this.isBlank()