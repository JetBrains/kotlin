/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractKotlinPsiChecker : Annotator, HighlightRangeExtension {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile as? KtFile ?: return

        if (!shouldHighlight(file)) return
        annotateElement(element, file, holder)
    }

    override fun isForceHighlightParents(file: PsiFile): Boolean {
        return file is KtFile
    }

    protected abstract fun shouldHighlight(file: KtFile): Boolean
    protected abstract fun annotateElement(element: PsiElement, containingFile: KtFile, holder: AnnotationHolder)
}
