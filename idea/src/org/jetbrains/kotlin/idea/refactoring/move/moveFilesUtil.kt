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

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.refactoring.isInJavaSourceRoot
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinAwareMoveFilesOrDirectoriesProcessor
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.KotlinAwareMoveFilesOrDirectoriesDialog
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

fun invokeMoveFilesOrDirectoriesRefactoring(
        moveDialog: KotlinAwareMoveFilesOrDirectoriesDialog?,
        project: Project,
        elements: Array<out PsiElement>,
        initialTargetDirectory: PsiDirectory?,
        moveCallback: MoveCallback?
) {
    fun closeDialog() {
        moveDialog?.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    project.executeCommand(MoveHandler.REFACTORING_NAME) {
        val selectedDir = (if (moveDialog != null) moveDialog.targetDirectory else initialTargetDirectory) ?: return@executeCommand
        val updatePackageDirective = moveDialog?.updatePackageDirective

        try {
            val choice = if (elements.size > 1 || elements[0] is PsiDirectory) intArrayOf(-1) else null
            val elementsToMove = elements
                    .filterNot {
                        it is PsiFile
                        && runWriteAction { CopyFilesOrDirectoriesHandler.checkFileExist(selectedDir, choice, it, it.name, "Move") }
                    }
                    .sortedWith( // process Kotlin files first so that light classes are updated before changing references in Java files
                            java.util.Comparator { o1, o2 ->
                                when {
                                    o1 is KtElement && o2 !is KtElement -> -1
                                    o1 !is KtElement && o2 is KtElement -> 1
                                    else -> 0
                                }
                            }
                    )

            elementsToMove.forEach {
                MoveFilesOrDirectoriesUtil.checkMove(it, selectedDir)
                if (it is KtFile && it.isInJavaSourceRoot()) {
                    it.updatePackageDirective = updatePackageDirective
                }
            }

            if (elementsToMove.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                val processor = KotlinAwareMoveFilesOrDirectoriesProcessor(
                        project,
                        elementsToMove as List<KtFile>,
                        selectedDir,
                        false,
                        false,
                        moveCallback,
                        Runnable(::closeDialog)
                )
                processor.run()
            }
            else {
                closeDialog()
            }
        }
        catch (e: IncorrectOperationException) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.message, "refactoring.moveFile", project)
        }
    }
}

// Mostly copied from MoveFilesOrDirectoriesUtil.doMove()
fun moveFilesOrDirectories(
        project: Project,
        elements: Array<PsiElement>,
        targetElement: PsiElement?,
        moveCallback: MoveCallback? = null
) {
    elements.forEach { if (it !is PsiFile && it !is PsiDirectory) throw IllegalArgumentException("unexpected element type: " + it) }

    val targetDirectory = MoveFilesOrDirectoriesUtil.resolveToDirectory(project, targetElement)
    if (targetElement != null && targetDirectory == null) return

    val initialTargetDirectory = MoveFilesOrDirectoriesUtil.getInitialTargetDirectory(targetDirectory, elements)

    fun doRun(moveDialog: KotlinAwareMoveFilesOrDirectoriesDialog?) {
        invokeMoveFilesOrDirectoriesRefactoring(moveDialog, project, elements, initialTargetDirectory, moveCallback)
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
        doRun(null)
        return
    }

    with(KotlinAwareMoveFilesOrDirectoriesDialog(project, ::doRun)) {
        setData(elements, initialTargetDirectory, "refactoring.moveFile")
        show()
    }
}