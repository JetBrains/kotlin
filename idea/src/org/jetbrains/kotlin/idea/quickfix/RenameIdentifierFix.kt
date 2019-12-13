/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.DataManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.rename.RenameHandlerRegistry

open class RenameIdentifierFix : LocalQuickFix {
    override fun getName() = "Rename"
    override fun getFamilyName() = name

    override fun startInWriteAction(): Boolean = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = element.containingFile ?: return
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return
        val editorManager = FileEditorManager.getInstance(project)
        val fileEditor = editorManager.getSelectedEditor(file.virtualFile) ?: return renameWithoutEditor(element)
        val dataContext = DataManager.getInstance().getDataContext(fileEditor.component)
        val renameHandler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext)

        val editor = editorManager.selectedTextEditor
        if (editor != null) {
            renameHandler?.invoke(project, editor, file, dataContext)
        } else {
            val elementToRename = getElementToRename(element) ?: return
            renameHandler?.invoke(project, arrayOf(elementToRename), dataContext)
        }
    }

    protected open fun getElementToRename(element: PsiElement): PsiElement? = element.parent

    private fun renameWithoutEditor(element: PsiElement) {
        val elementToRename = getElementToRename(element) ?: return
        val factory = RefactoringFactory.getInstance(element.project)
        val renameRefactoring = factory.createRename(elementToRename, null, true, true)
        renameRefactoring.run()
    }
}