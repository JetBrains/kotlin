/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReference
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandlerDelegate
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesImpl
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import org.jetbrains.kotlin.psi.*
import java.util.*

class MoveKotlinDeclarationsHandler : MoveHandlerDelegate() {
    private fun getSourceDirectories(elements: Array<out PsiElement>) = elements.mapTo(LinkedHashSet()) { it.containingFile?.parent }

    private fun doMoveWithCheck(
            project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?, editor: Editor?
    ): Boolean {
        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, elements.toList(), true)) return false
        if (getSourceDirectories(elements).singleOrNull() == null) {
            CommonRefactoringUtil.showErrorHint(
                    project, editor, "All declarations must belong to the same directory", MOVE_DECLARATIONS, null
            )
            return false
        }

        val elementsToSearch = elements.mapTo(LinkedHashSet()) { it as KtNamedDeclaration }

        if (elementsToSearch.any { it.parent !is KtFile }) {
            val message = RefactoringBundle.getCannotRefactorMessage("Move declaration is only supported for top-level declarations")
            CommonRefactoringUtil.showErrorHint(project, editor, message, MOVE_DECLARATIONS, null)
            return true
        }

        val targetPackageName = MoveClassesOrPackagesImpl.getInitialTargetPackageName(targetContainer, elements)
        val targetDirectory = MoveClassesOrPackagesImpl.getInitialTargetDirectory(targetContainer, elements)
        val searchInComments = JavaRefactoringSettings.getInstance()!!.MOVE_SEARCH_IN_COMMENTS
        val searchInText = JavaRefactoringSettings.getInstance()!!.MOVE_SEARCH_FOR_TEXT
        val targetFile = targetContainer as? KtFile
        val moveToPackage = targetContainer !is KtFile

        MoveKotlinTopLevelDeclarationsDialog(
                project, elementsToSearch, targetPackageName, targetDirectory, targetFile, moveToPackage, searchInComments, searchInText, callback
        ).show()

        return true
    }

    private fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?, editorMode: Boolean): Boolean {
        if (!super.canMove(elements, targetContainer)) return false
        if (getSourceDirectories(elements).singleOrNull() == null) return false

        return elements.all { e ->
            if (e is KtClass || (e is KtObjectDeclaration && !e.isObjectLiteral()) || e is KtNamedFunction || e is KtProperty) {
                (editorMode || e.parent is KtFile) && e.canRefactor()
            }
            else false
        }
    }

    override fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?): Boolean {
        return canMove(elements, targetContainer, false)
    }

    override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>): Boolean {
        return psiElement is PsiPackage || (psiElement is PsiDirectory && psiElement.getPackage() != null) || psiElement is KtFile
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        doMoveWithCheck(project, elements, targetContainer, callback, null)
    }

    override fun tryToMove(
            element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        val elementsToMove = arrayOf(element)
        val targetContainer = dataContext?.let { dataContext -> LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext) }
        return canMove(elementsToMove, targetContainer, true) && doMoveWithCheck(project, elementsToMove, targetContainer, null, editor)
    }
}

private val MOVE_DECLARATIONS = "Move Declarations"