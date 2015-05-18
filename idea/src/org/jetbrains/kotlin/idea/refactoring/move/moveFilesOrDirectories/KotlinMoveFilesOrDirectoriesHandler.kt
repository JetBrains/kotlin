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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler
import org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile

public class KotlinMoveFilesOrDirectoriesHandler : MoveFilesOrDirectoriesHandler() {
    override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
        val adjustedElements = elements.map {
            when {
                it is PsiFile, it is PsiDirectory -> it
                it is JetClassOrObject && it.getParent() is JetFile -> it.getParent()
                else -> return false
            }
        }
        return super.canMove(adjustedElements.toTypedArray(), targetContainer)
    }

    override fun adjustForMove(project: Project, sourceElements: Array<out PsiElement>, targetElement: PsiElement?): Array<PsiElement> {
        return sourceElements.map { if (it is JetClassOrObject) it.getContainingFile() else it }.toTypedArray()
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        if (!(targetContainer == null || targetContainer is PsiDirectory || targetContainer is PsiDirectoryContainer)) return

        moveFilesOrDirectories(project, adjustForMove(project, elements, targetContainer), targetContainer) {
            callback?.refactoringCompleted()
        }
    }

    companion object {
        public fun isMovableClass(clazz: JetClassOrObject): Boolean {
            return clazz.isTopLevel() && clazz.getContainingJetFile().getDeclarations().all { it !is JetClassOrObject || it == clazz }
        }
    }
}
