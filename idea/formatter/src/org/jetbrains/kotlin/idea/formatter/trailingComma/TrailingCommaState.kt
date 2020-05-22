/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.trailingComma

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.containsLineBreakInThis
import org.jetbrains.kotlin.idea.util.isMultiline
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

enum class TrailingCommaState {
    /**
     * The trailing comma is needed and exists
     */
    EXISTS,

    /**
     * The trailing comma is needed and doesn't exists
     */
    MISSING,

    /**
     * The trailing comma isn't needed and doesn't exists
     */
    NOT_EXISTS,

    /**
     * The trailing comma isn't needed, but exists
     */
    REDUNDANT,

    /**
     * The trailing comma isn't applicable for this element
     */
    NOT_APPLICABLE,
    ;

    companion object {
        fun stateForElement(element: PsiElement): TrailingCommaState = when {
            element !is KtElement || !element.canAddTrailingComma() -> NOT_APPLICABLE
            isMultiline(element) ->
                if (TrailingCommaHelper.trailingCommaExists(element))
                    EXISTS
                else
                    MISSING
            else ->
                if (TrailingCommaHelper.trailingCommaExists(element))
                    REDUNDANT
                else
                    NOT_EXISTS
        }
    }
}

val TrailingCommaState.existsOrMissing: Boolean
    get() = when (this) {
        TrailingCommaState.EXISTS, TrailingCommaState.MISSING -> true
        else -> false
    }

private fun isMultiline(ktElement: KtElement): Boolean = when {
    ktElement.parent is KtFunctionLiteral -> isMultiline(ktElement.parent as KtElement)

    ktElement is KtFunctionLiteral -> ktElement.isMultiline(
        startOffsetGetter = { valueParameterList?.startOffset },
        endOffsetGetter = { arrow?.endOffset },
    )

    ktElement is KtWhenEntry -> ktElement.isMultiline(
        startOffsetGetter = { startOffset },
        endOffsetGetter = { arrow?.endOffset },
    )

    ktElement is KtDestructuringDeclaration -> ktElement.isMultiline(
        startOffsetGetter = { lPar?.startOffset },
        endOffsetGetter = { rPar?.endOffset },
    )

    else -> ktElement.isMultiline()
}

private fun <T : PsiElement> T.isMultiline(
    startOffsetGetter: T.() -> Int?,
    endOffsetGetter: T.() -> Int?,
): Boolean {
    val startOffset = startOffsetGetter() ?: startOffset
    val endOffset = endOffsetGetter() ?: endOffset
    return containsLineBreakInThis(startOffset, endOffset)
}
