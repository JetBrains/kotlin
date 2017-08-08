/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractClass

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.extractSuperclass.ExtractSuperClassUtil
import com.intellij.refactoring.lang.ElementsHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.idea.refactoring.SeparateFileWrapper
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ui.KotlinExtractSuperDialogBase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class KotlinExtractSuperHandlerBase(private val isExtractInterface: Boolean) : RefactoringActionHandler, ElementsHandler {
    override fun isEnabledOnElements(elements: Array<out PsiElement>) = elements.singleOrNull() is KtClassOrObject

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return
        val klass = element.getNonStrictParentOfType<KtClassOrObject>() ?: return
        if (!checkClass(klass, editor)) return
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        selectElements(klass, project, editor)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        if (dataContext == null) return
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        val klass = PsiTreeUtil.findCommonParent(*elements)?.getNonStrictParentOfType<KtClassOrObject>() ?: return
        if (!checkClass(klass, editor)) return
        selectElements(klass, project, editor)
    }

    private fun checkClass(klass: KtClassOrObject, editor: Editor?): Boolean {
        val project = klass.project

        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, klass)) return false

        getErrorMessage(klass)?.let {
            CommonRefactoringUtil.showErrorHint(
                    project,
                    editor,
                    RefactoringBundle.getCannotRefactorMessage(it),
                    KotlinExtractSuperclassHandler.REFACTORING_NAME,
                    HelpID.EXTRACT_SUPERCLASS
            )
            return false
        }

        return true
    }

    private fun selectElements(klass: KtClassOrObject, project: Project, editor: Editor?) {
        val containers = klass.getExtractionContainers(strict = true, includeAll = true) + SeparateFileWrapper(klass.manager)

        if (editor == null) return doInvoke(klass, containers.first(), project, editor)

        chooseContainerElementIfNecessary(
                containers,
                editor,
                if (containers.first() is KtFile) "Select target file" else "Select target code block / file",
                true,
                { it },
                { doInvoke(klass, if (it is SeparateFileWrapper) klass.containingFile.parent!! else it, project, editor) }
        )
    }

    protected fun checkConflicts(originalClass: KtClassOrObject, dialog: KotlinExtractSuperDialogBase): Boolean {
        val conflicts = ExtractSuperRefactoring.collectConflicts(
                originalClass,
                dialog.selectedMembers,
                dialog.selectedTargetParent,
                dialog.extractedSuperName,
                isExtractInterface
        )
        return ExtractSuperClassUtil.showConflicts(dialog, conflicts, originalClass.project)
    }

    internal abstract fun getErrorMessage(klass: KtClassOrObject): String?

    protected abstract fun doInvoke(klass: KtClassOrObject, targetParent: PsiElement, project: Project, editor: Editor?)
}