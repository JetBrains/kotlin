/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories
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

    override fun tryToMove(
        element: PsiElement,
        project: Project,
        dataContext: DataContext?,
        reference: PsiReference?,
        editor: Editor?
    ): Boolean {
        if (element is KtLightClassForFacade) {
            doMove(project, element.files.toTypedArray(), dataContext?.getData(LangDataKeys.TARGET_PSI_ELEMENT), null)
            return true
        }

        return super.tryToMove(element, project, dataContext, reference, editor)
    }
}
