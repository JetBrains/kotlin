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
import com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ToRawStringLiteralIntention : SelfTargetingOffsetIndependentIntention<KtStringTemplateExpression>(
    KtStringTemplateExpression::class.java,
    "To raw string literal"
), LowPriorityAction {
    override fun isApplicableTo(element: KtStringTemplateExpression): Boolean {
        val text = element.text
        if (text.startsWith("\"\"\"")) return false // already raw

        val escapeEntries = element.entries.filterIsInstance<KtEscapeStringTemplateEntry>()
        for (entry in escapeEntries) {
            val c = entry.unescapedValue.singleOrNull() ?: return false
            if (Character.isISOControl(c) && c != '\n' && c != '\r') return false
        }

        val converted = convertContent(element)
        return !converted.contains("\"\"\"") && !hasTrailingSpaces(converted)
    }

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val startOffset = element.startOffset
        val endOffset = element.endOffset
        val currentOffset = editor?.caretModel?.currentCaret?.offset ?: startOffset

        val text = convertContent(element)
        val replaced = element.replaced(KtPsiFactory(element).createExpression("\"\"\"" + text + "\"\"\""))

        val offset = when {
            startOffset == currentOffset -> startOffset
            endOffset == currentOffset -> replaced.endOffset
            else -> minOf(currentOffset + 2, replaced.endOffset)
        }
        editor?.caretModel?.moveToOffset(offset)
    }

    private fun convertContent(element: KtStringTemplateExpression): String {
        val text = buildString {
            val entries = element.entries
            for ((index, entry) in entries.withIndex()) {
                val value = entry.value()

                if (value.endsWith("$") && index < entries.size - 1) {
                    val nextChar = entries[index + 1].value().first()
                    if (nextChar.isJavaIdentifierStart() || nextChar == '{') {
                        append("\${\"$\"}")
                        continue
                    }
                }

                append(value)
            }
        }
        return StringUtilRt.convertLineSeparators(text, "\n")
    }

    private fun hasTrailingSpaces(text: String): Boolean {
        var afterSpace = true
        for (c in text) {
            if ((c == '\n' || c == '\r') && afterSpace) return true
            afterSpace = c == ' ' || c == '\t'
        }
        return false
    }

    private fun KtStringTemplateEntry.value() = if (this is KtEscapeStringTemplateEntry) this.unescapedValue else text
}