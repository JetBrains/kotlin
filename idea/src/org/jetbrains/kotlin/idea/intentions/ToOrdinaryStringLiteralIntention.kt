/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ToOrdinaryStringLiteralIntention : SelfTargetingOffsetIndependentIntention<KtStringTemplateExpression>(
    KtStringTemplateExpression::class.java,
    "To ordinary string literal"
), LowPriorityAction {
    companion object {
        private val TRIM_INDENT_FUNCTIONS = listOf(FqName("kotlin.text.trimIndent"), FqName("kotlin.text.trimMargin"))
    }

    override fun isApplicableTo(element: KtStringTemplateExpression): Boolean {
        return element.text.startsWith("\"\"\"")
    }

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val startOffset = element.startOffset
        val endOffset = element.endOffset
        val currentOffset = editor?.caretModel?.currentCaret?.offset ?: startOffset

        val entries = element.entries
        val trimIndentCall = getTrimIndentCall(element, entries)
        val text = buildString {
            append("\"")
            if (trimIndentCall != null) {
                append(trimIndentCall.stringTemplateText)
            } else {
                entries.joinTo(buffer = this, separator = "") {
                    if (it is KtLiteralStringTemplateEntry) it.text.escape() else it.text
                }
            }
            append("\"")
        }
        val replaced = (trimIndentCall?.qualifiedExpression ?: element).replaced(KtPsiFactory(element).createExpression(text))

        val offset = when {
            currentOffset - startOffset < 2 -> startOffset
            endOffset - currentOffset < 2 -> replaced.endOffset
            else -> maxOf(currentOffset - 2, replaced.startOffset)
        }
        editor?.caretModel?.moveToOffset(offset)
    }

    private fun String.escape(): String {
        var text = this
        text = text.replace("\\", "\\\\")
        text = text.replace("\"", "\\\"")
        return StringUtil.convertLineSeparators(text, "\\n")
    }

    private fun getTrimIndentCall(
        element: KtStringTemplateExpression,
        entries: Array<KtStringTemplateEntry>
    ): TrimIndentCall? {
        val qualifiedExpression = element.getQualifiedExpressionForReceiver()?.takeIf {
            it.callExpression?.isCalling(TRIM_INDENT_FUNCTIONS) == true
        } ?: return null

        val marginPrefix = if (qualifiedExpression.calleeName == "trimMargin") {
            when (val arg = qualifiedExpression.callExpression?.valueArguments?.singleOrNull()?.getArgumentExpression()) {
                null -> "|"
                is KtStringTemplateExpression -> arg.entries.singleOrNull()?.takeIf { it is KtLiteralStringTemplateEntry }?.text
                else -> null
            } ?: return null
        } else {
            null
        }

        val stringTemplateText = entries
            .joinToString(separator = "") { it.text }
            .let { if (marginPrefix != null) it.trimMargin(marginPrefix) else it.trimIndent() }
            .escape()

        return TrimIndentCall(qualifiedExpression, stringTemplateText)
    }

    private data class TrimIndentCall(
        val qualifiedExpression: KtQualifiedExpression,
        val stringTemplateText: String
    )
}