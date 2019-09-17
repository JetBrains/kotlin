/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.allowResolveExpectedToBeCached
import org.jetbrains.kotlin.psi.KtFile

abstract class KotlinQuickFixAction<out T : PsiElement>(element: T) : QuickFixActionBase<T>(element) {
    protected open fun isAvailable(project: Project, editor: Editor?, file: KtFile) = true

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val ktFile = file as? KtFile ?: return false
        return allowResolveExpectedToBeCached {
            // Quick fixes availability is checked in UI thread but only after background highlighting pass is finished
            // so we are expecting that every relevant results are already cached.
            isAvailable(project, editor, ktFile)
        }
    }

    final override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val element = element ?: return
        if (file is KtFile && FileModificationService.getInstance().prepareFileForWrite(element.containingFile)) {
            invoke(project, editor, file)
        }
    }

    protected abstract operator fun invoke(project: Project, editor: Editor?, file: KtFile)

    override fun startInWriteAction() = true
}
