/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.psi.AbstractElementManipulator
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.openapi.util.text.StringUtil
import kotlin.jvm.java
import kotlin.text.substring

class StringTemplateExpressionManipulator : AbstractElementManipulator<KtStringTemplateExpression>() {
    override fun handleContentChange(
        element: KtStringTemplateExpression,
        range: TextRange,
        newContent: String
    ): KtStringTemplateExpression? {
        val node = element.node
        val content = if (node.firstChildNode.textLength == 1) StringUtil.escapeStringCharacters(newContent) else newContent
        val oldText = node.text
        val newText = oldText.substring(0, range.startOffset) + content + oldText.substring(range.endOffset)
        val expression = KtPsiFactory(element.project).createExpression(newText)
        node.replaceAllChildrenToChildrenOf(expression.node)
        return node.getPsi(KtStringTemplateExpression::class.java)
    }

    override fun getRangeInElement(element: KtStringTemplateExpression): TextRange {
        return element.getContentRange()
    }
}
