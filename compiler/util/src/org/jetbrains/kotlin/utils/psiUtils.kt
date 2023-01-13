/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

fun getElementTextWithContext(psiElement: PsiElement): String {
    if (!psiElement.isValid) return "<invalid element $psiElement>"

    if (psiElement is PsiFile) {
        return psiElement.containingFile.text
    }

    // Find parent for element among file children
    val topLevelElement = PsiTreeUtil.findFirstParent(psiElement) { it.parent is PsiFile }
        ?: throw AssertionError("For non-file element we should always be able to find parent in file children")

    val startContextOffset = topLevelElement.textRange.startOffset
    val elementContextOffset = psiElement.textRange.startOffset

    val inFileParentOffset = elementContextOffset - startContextOffset


    val containingFile = psiElement.containingFile
    val isInjected = containingFile is VirtualFileWindow
    return StringBuilder(topLevelElement.text)
        .insert(inFileParentOffset, "<caret>")
        .insert(0, "File name: ${containingFile.name} Physical: ${containingFile.isPhysical} Injected: $isInjected\n")
        .toString()
}