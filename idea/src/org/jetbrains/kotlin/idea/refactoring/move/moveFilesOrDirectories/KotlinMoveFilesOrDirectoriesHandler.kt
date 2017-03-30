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

package org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

class KotlinMoveFilesOrDirectoriesHandler : MoveFilesOrDirectoriesHandler() {
    private fun adjustElements(elements: Array<out PsiElement>): Array<PsiElement>? {
        return elements.map {
            when {
                it is PsiFile || it is PsiDirectory -> it
                it is PsiClass && it.containingClass == null -> it.containingFile
                it is KtClassOrObject && it.parent is KtFile -> it.parent
                else -> return null
            }
        }.toTypedArray()
    }

    override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
        val adjustedElements = adjustElements(elements) ?: return false
        if (adjustedElements.none { it is KtFile }) return false

        return super.canMove(adjustedElements, targetContainer)
    }

    override fun adjustForMove(project: Project, sourceElements: Array<out PsiElement>, targetElement: PsiElement?): Array<PsiElement>? {
        return adjustElements(sourceElements)
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        if (!(targetContainer == null || targetContainer is PsiDirectory || targetContainer is PsiDirectoryContainer)) return

        moveFilesOrDirectories(
                project,
                adjustForMove(project, elements, targetContainer) ?: return,
                targetContainer,
                callback?.let { MoveCallback { it.refactoringCompleted() } }
        )
    }

    override fun tryToMove(element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?): Boolean {
        if (element is KtLightClassForFacade) {
            doMove(project, element.files.toTypedArray(), dataContext?.getData(LangDataKeys.TARGET_PSI_ELEMENT), null)
            return true
        }

        return super.tryToMove(element, project, dataContext, reference, editor)
    }
}
