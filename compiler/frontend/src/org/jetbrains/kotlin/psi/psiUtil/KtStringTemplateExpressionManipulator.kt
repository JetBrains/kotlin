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

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.AbstractElementManipulator
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KtStringTemplateExpressionManipulator : AbstractElementManipulator<KtStringTemplateExpression>() {
    override fun handleContentChange(
        element: KtStringTemplateExpression,
        range: TextRange,
        newContent: String
    ): KtStringTemplateExpression? {
        val node = element.node
        val oldText = node.text

        fun wrapAsInOld(content: String) = oldText.substring(0, range.startOffset) + content + oldText.substring(range.endOffset)

        fun makeKtStringTemplateExpressionFromText(text: String) =
            KtPsiFactory(element.project).createExpression(text) as? KtStringTemplateExpression
                    ?: error("can't create a `KtStringTemplateExpression` from '$text'")

        if (element.isSingleQuoted()) {
            val templateFromNewContent = makeKtStringTemplateExpressionFromText("\"\"\"" + newContent + "\"\"\"")

            val escapedText = templateFromNewContent.entries.joinToString("") { entry ->
                when (entry) {
                    is KtStringTemplateEntryWithExpression -> entry.text
                    else -> StringUtil.escapeStringCharacters(entry.text)
                }
            }

            val escaped = makeKtStringTemplateExpressionFromText(wrapAsInOld(escapedText))
            node.replaceAllChildrenToChildrenOf(escaped.node)
        } else {
            val templateFromNewContent = makeKtStringTemplateExpressionFromText(wrapAsInOld(newContent))
            node.replaceAllChildrenToChildrenOf(templateFromNewContent.node)
        }

        return node.getPsi(KtStringTemplateExpression::class.java)
    }

    override fun getRangeInElement(element: KtStringTemplateExpression): TextRange {
        return element.getContentRange()
    }
}
