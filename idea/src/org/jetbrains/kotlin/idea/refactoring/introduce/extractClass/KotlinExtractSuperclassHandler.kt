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
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ui.KotlinExtractSuperclassDialog
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

object KotlinExtractSuperclassHandler : RefactoringActionHandler, ElementsHandler {
    val REFACTORING_NAME = "Extract Superclass"

    override fun isEnabledOnElements(elements: Array<out PsiElement>) = elements.singleOrNull() is KtClassOrObject

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return
        val klass = element.getNonStrictParentOfType<KtClassOrObject>() ?: return
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        selectElements(klass, project, editor)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        if (dataContext == null) return
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        val klass = PsiTreeUtil.findCommonParent(*elements)?.getNonStrictParentOfType<KtClassOrObject>() ?: return
        selectElements(klass, project, editor)
    }

    fun selectElements(klass: KtClassOrObject, project: Project, editor: Editor?) {
        val containers = klass.getExtractionContainers(strict = true, includeAll = true) + SeparateFileWrapper(klass.manager)

        if (editor == null) return doInvoke(klass, containers.first(), project, editor)

        chooseContainerElementIfNecessary(
                containers,
                editor,
                if (containers.first() is KtFile) "Select target file" else "Select target code block / file",
                true,
                { it },
                { doInvoke(klass, it, project, editor) }
        )
    }

    fun getErrorMessage(klass: KtClassOrObject): String? {
        if (klass is KtClass) {
            if (klass.isInterface()) return RefactoringBundle.message("superclass.cannot.be.extracted.from.an.interface")
            if (klass.isEnum()) return RefactoringBundle.message("superclass.cannot.be.extracted.from.an.enum")
            if (klass.isAnnotation()) return "Superclass cannot be extracted from an annotation class"
        }
        return null
    }

    private fun checkConflicts(originalClass: KtClassOrObject, dialog: KotlinExtractSuperclassDialog): Boolean {
        val conflicts = ExtractSuperclassRefactoring.collectConflicts(
                originalClass,
                dialog.selectedMembers,
                dialog.selectedTargetParent,
                dialog.extractedSuperName
        )
        return ExtractSuperClassUtil.showConflicts(dialog, conflicts, originalClass.project)
    }

    private fun doInvoke(klass: KtClassOrObject, container: PsiElement, project: Project, editor: Editor?) {
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, klass)) return

        getErrorMessage(klass)?.let {
            CommonRefactoringUtil.showErrorHint(project, editor, RefactoringBundle.getCannotRefactorMessage(it), REFACTORING_NAME, HelpID.EXTRACT_SUPERCLASS)
        }

        val targetParent = (if (container is SeparateFileWrapper) klass.containingFile.parent else container) ?: return

        KotlinExtractSuperclassDialog(
                originalClass = klass,
                targetParent = targetParent,
                conflictChecker = { checkConflicts(klass, it) },
                refactoring = { ExtractSuperclassRefactoring(it).performRefactoring() }
        ).show()
    }
}