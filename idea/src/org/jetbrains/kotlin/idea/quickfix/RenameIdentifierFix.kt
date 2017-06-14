/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        }
        else {
            val elementToRename = getElementToRename(element) ?: return
            renameHandler?.invoke(project, arrayOf(elementToRename), dataContext)
        }
    }

    protected open fun getElementToRename(element: PsiElement): PsiElement? = element.parent

    private fun renameWithoutEditor(element: PsiElement) {
        val elementToRename = getElementToRename(element)
        val factory = RefactoringFactory.getInstance(element.project)
        val renameRefactoring = factory.createRename(elementToRename, null, true, true)
        renameRefactoring.run()
    }
}