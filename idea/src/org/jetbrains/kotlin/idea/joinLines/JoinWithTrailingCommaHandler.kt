/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.joinLines

import com.intellij.codeInsight.editorActions.JoinLinesHandlerDelegate
import com.intellij.codeInsight.editorActions.JoinLinesHandlerDelegate.CANNOT_JOIN
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.util.containsLineBreakInRange
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaHelper
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaState
import org.jetbrains.kotlin.idea.formatter.trailingComma.addTrailingCommaIsAllowedForThis
import org.jetbrains.kotlin.idea.formatter.trailingComma.existsOrMissing
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class JoinWithTrailingCommaHandler : JoinLinesHandlerDelegate {
    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return CANNOT_JOIN
        val commaOwner = file.findElementAt(start)
            ?.parentsWithSelf
            ?.filter { !document.containsLineBreakInRange(it.textRange) }
            ?.findLast { it.addTrailingCommaIsAllowedForThis() } as? KtElement
            ?: return CANNOT_JOIN

        if (TrailingCommaState.stateForElement(commaOwner).existsOrMissing) return CANNOT_JOIN
        val result = CodeStyleManager.getInstance(file.project).reformat(commaOwner) as KtElement
        return TrailingCommaHelper.elementAfterLastElement(result)?.startOffset ?: end - 1
    }

}
