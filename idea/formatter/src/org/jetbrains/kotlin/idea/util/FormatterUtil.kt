/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.formatting.ASTBlock
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/*
 * ASTBlock.node is nullable, this extension was introduced to minimize changes
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

/**
 * Can be removed with all usages after moving master to 1.3 with new default code style settings.
 */
val isDefaultOfficialCodeStyle by lazy { !KotlinCodeStyleSettings.defaultSettings().CONTINUATION_INDENT_FOR_CHAINED_CALLS }

fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { file -> file.viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(file) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: "") + 1
}

fun PsiElement.isMultiline() = getLineCount() > 1

fun KtFunctionLiteral.needTrailingComma(settings: CodeStyleSettings): Boolean = valueParameterList?.trailingComma != null ||
        settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA &&
        run(fun(): Boolean {
            val startOffset = valueParameterList?.startOffset ?: return false
            val endOffset = arrow?.endOffset ?: return false
            return containsLineBreakInThis(startOffset, endOffset)
        })

fun KtWhenEntry.needTrailingComma(settings: CodeStyleSettings): Boolean = trailingComma != null ||
        !isElse &&
        settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA &&
        parent.safeAs<KtWhenExpression>()?.leftParenthesis != null &&
        run(fun(): Boolean {
            val endOffset = arrow?.endOffset ?: return false
            return containsLineBreakInThis(startOffset, endOffset)
        })

fun PsiElement.containsLineBreakInThis(globalStartOffset: Int, globalEndOffset: Int): Boolean {
    val textRange = TextRange.create(globalStartOffset, globalEndOffset).shiftLeft(startOffset)
    return StringUtil.containsLineBreak(textRange.subSequence(text))
}