/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.Editor
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class IndentRawStringIntention : SelfTargetingIntention<KtStringTemplateExpression>(
    KtStringTemplateExpression::class.java, "Indent raw string"
) {

    override fun isApplicableTo(element: KtStringTemplateExpression, caretOffset: Int): Boolean {
        if (!element.text.startsWith("\"\"\"")) return false
        if (element.parents.any { it is KtAnnotationEntry || (it as? KtProperty)?.hasModifier(KtTokens.CONST_KEYWORD) == true }) return false
        if (element.getQualifiedExpressionForReceiver() != null) return false
        val entries = element.entries
        if (entries.size <= 1 || entries.any { it.text.startsWith(" ") || it.text.startsWith("\t") }) return false
        return true
    }

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val file = element.containingKtFile
        val project = file.project
        val indentOptions = CodeStyle.getIndentOptions(file)
        val parentIndent = CodeStyleManager.getInstance(project).getLineIndent(file, element.parent.startOffset) ?: ""
        val indent = if (indentOptions.USE_TAB_CHARACTER) "$parentIndent\t" else "$parentIndent${" ".repeat(indentOptions.INDENT_SIZE)}"

        val newString = buildString {
            val maxIndex = element.entries.size - 1
            element.entries.forEachIndexed { index, entry ->
                if (index == 0) append("\n$indent")
                append(entry.text)
                if (entry.text == "\n") append(indent)
                if (index == maxIndex) append("\n$indent")
            }
        }

        element.replace(KtPsiFactory(element).createExpression("\"\"\"$newString\"\"\".trimIndent()"))
    }

}