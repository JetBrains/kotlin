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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations

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
import org.jetbrains.kotlin.idea.core.refactoring.isInJavaSourceRoot
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import org.jetbrains.kotlin.psi.*
import java.util.LinkedHashSet

public class MoveKotlinTopLevelDeclarationsHandler : MoveHandlerDelegate() {
    private fun getSourceDirectories(elements: Array<out PsiElement>) = elements.mapTo(LinkedHashSet()) { it.containingFile?.parent }

    private fun doMoveWithCheck(
            project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?
    ): Boolean {
        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, elements.toList(), true)) return false
        if (getSourceDirectories(elements).singleOrNull() == null) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("move.title"),
                                                   "All declarations must belong to the same directory",
                                                   null,
                                                   project)
            return false
        }

        val elementsToSearch = elements.mapTo(LinkedHashSet()) { it as JetNamedDeclaration }

        val targetPackageName = MoveClassesOrPackagesImpl.getInitialTargetPackageName(targetContainer, elements)
        val targetDirectory = MoveClassesOrPackagesImpl.getInitialTargetDirectory(targetContainer, elements)
        val searchInComments = JavaRefactoringSettings.getInstance()!!.MOVE_SEARCH_IN_COMMENTS
        val searchInText = JavaRefactoringSettings.getInstance()!!.MOVE_SEARCH_FOR_TEXT
        val targetFile = targetContainer as? JetFile
        val moveToPackage = targetContainer !is JetFile

        MoveKotlinTopLevelDeclarationsDialog(
                project, elementsToSearch, targetPackageName, targetDirectory, targetFile, moveToPackage, searchInComments, searchInText, callback
        ).show()

        return true
    }

    override fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?): Boolean {
        if (!super.canMove(elements, targetContainer)) return false
        if (getSourceDirectories(elements).singleOrNull() == null) return false

        return elements.all { e ->
            if (e is JetClass || (e is JetObjectDeclaration && !e.isObjectLiteral()) || e is JetNamedFunction || e is JetProperty) {
                val parent = e.parent
                parent is JetFile && parent.isInJavaSourceRoot()
            }
            else false
        }
    }

    override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>): Boolean {
        return psiElement is PsiPackage || (psiElement is PsiDirectory && psiElement.getPackage() != null) || psiElement is JetFile
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        doMoveWithCheck(project, elements, targetContainer, callback)
    }

    override fun tryToMove(
            element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        val elementsToMove = arrayOf(element)
        val targetContainer = dataContext?.let { dataContext -> LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext) }

        return canMove(elementsToMove, targetContainer) && doMoveWithCheck(project, elementsToMove, targetContainer, null)
    }
}
