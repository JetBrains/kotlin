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
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/*
 * ASTBlock.node is nullable, this extension was introduced to minimize changes
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

/**
 * Can be removed with all usages after moving master to 1.3 with new default code style settings.
 */
val isDefaultOfficialCodeStyle by lazy { !KotlinCodeStyleSettings.defaultSettings().CONTINUATION_INDENT_FOR_CHAINED_CALLS }

fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength && spaceRange.startOffset < spaceRange.endOffset) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset - 1)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: "") + 1
}

fun PsiElement.isMultiline() = getLineCount() > 1

fun KtFunctionLiteral.needTrailingComma(settings: CodeStyleSettings, checkTrailingComma: Boolean = true): Boolean = needTrailingComma(
    settings = settings,
    trailingComma = { if (checkTrailingComma) valueParameterList?.trailingComma else null },
    globalStartOffset = { valueParameterList?.startOffset },
    globalEndOffset = { arrow?.endOffset }
)

fun KtWhenEntry.needTrailingComma(settings: CodeStyleSettings, checkTrailingComma: Boolean = true): Boolean = needTrailingComma(
    settings = settings,
    trailingComma = { if (checkTrailingComma) trailingComma else null },
    additionalCheck = { !isElse },
    globalEndOffset = { arrow?.endOffset }
)

fun KtDestructuringDeclaration.needTrailingComma(settings: CodeStyleSettings, checkTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkTrailingComma) trailingComma else null },
        globalStartOffset = { lPar?.startOffset },
        globalEndOffset = { rPar?.endOffset }
    )

fun <T : PsiElement> T.needTrailingComma(
    settings: CodeStyleSettings,
    trailingComma: T.() -> PsiElement?,
    additionalCheck: () -> Boolean = { true },
    globalStartOffset: T.() -> Int? = PsiElement::startOffset,
    globalEndOffset: T.() -> Int? = PsiElement::endOffset
) = (trailingComma() != null || settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA) && additionalCheck() && run(fun(): Boolean {
    val startOffset = globalStartOffset() ?: return false
    val endOffset = globalEndOffset() ?: return false
    return containsLineBreakInThis(startOffset, endOffset)
})

fun PsiElement.containsLineBreakInThis(globalStartOffset: Int, globalEndOffset: Int): Boolean {
    val textRange = TextRange.create(globalStartOffset, globalEndOffset).shiftLeft(startOffset)
    return StringUtil.containsLineBreak(textRange.subSequence(text))
}