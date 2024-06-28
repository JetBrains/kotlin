/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.AbstractElementManipulator
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

private val LOG = Logger.getInstance(KtStringTemplateExpressionManipulator::class.java)

class KtStringTemplateExpressionManipulator : AbstractElementManipulator<KtStringTemplateExpression>() {
    override fun handleContentChange(
        element: KtStringTemplateExpression,
        range: TextRange,
        newContent: String
    ): KtStringTemplateExpression? {
        val node = element.node
        val oldText = node.text

        fun wrapAsInOld(content: String) = oldText.substring(0, range.startOffset) + content + oldText.substring(range.endOffset)

        fun makeKtExpressionFromText(text: String): KtExpression {
            val ktExpression = KtPsiFactory(element.project).createExpression(text)
            if (ktExpression !is KtStringTemplateExpression) {
                LOG.error("can't create a `KtStringTemplateExpression` from '$text'")
            }
            return ktExpression
        }

        val newContentPreprocessed: String =
            if (element.isSingleQuoted()) {
                val expressionFromText = makeKtExpressionFromText("\"\"\"$newContent\"\"\"")
                if (expressionFromText is KtStringTemplateExpression) {
                    expressionFromText.entries.joinToString("") { entry ->
                        when (entry) {
                            is KtStringTemplateEntryWithExpression -> entry.text
                            else -> StringUtil.escapeStringCharacters(entry.text)
                        }
                    }
                } else newContent
            } else newContent

        val newKtExpression = makeKtExpressionFromText(wrapAsInOld(newContentPreprocessed))
        node.replaceAllChildrenToChildrenOf(newKtExpression.node)

        return node.getPsi(KtStringTemplateExpression::class.java)
    }

    override fun getRangeInElement(element: KtStringTemplateExpression): TextRange {
        return element.getContentRange()
    }
}