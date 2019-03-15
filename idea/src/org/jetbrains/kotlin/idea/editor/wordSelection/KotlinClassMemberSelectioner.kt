/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace

class KotlinClassMemberSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean {
        return e.parent is KtClassBody
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val parent = e.parent
        val firstChild = parent.firstChild ?: return null
        val lastChild = parent.lastChild ?: return null
        val startElement = firstChild.getNextSiblingIgnoringWhitespace() ?: firstChild
        val endElement = lastChild.getPrevSiblingIgnoringWhitespace() ?: lastChild
        val textRange = TextRange(startElement.textRange.startOffset, endElement.textRange.endOffset)
        return expandToWholeLinesWithBlanks(editorText, textRange)
    }
}