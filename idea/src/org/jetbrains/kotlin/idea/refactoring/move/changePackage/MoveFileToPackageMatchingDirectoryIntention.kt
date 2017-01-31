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

package org.jetbrains.kotlin.idea.refactoring.move.changePackage

import com.intellij.CommonBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.util.RefactoringMessageUtil
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.refactoring.isInsideInjectedFragment
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtPackageDirective

class MoveFileToPackageMatchingDirectoryIntention : SelfTargetingOffsetIndependentIntention<KtPackageDirective>(
        KtPackageDirective::class.java, "", "Move file to package-matching directory"
) {
    override fun isApplicableTo(element: KtPackageDirective): Boolean {
        if (element.isInsideInjectedFragment) return false
        if (element.getContainingKtFile().packageMatchesDirectory()) return false

        val qualifiedName = element.qualifiedName
        val dirName = if (qualifiedName.isEmpty()) "source root" else "'${qualifiedName.replace('.', '/')}'"
        text = "Move file to $dirName"
        return true
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtPackageDirective, editor: Editor?) {
        val file = element.getContainingKtFile()
        val project = file.project

        val sourceRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(project)
        val packageWrapper = PackageWrapper(PsiManager.getInstance(project), element.qualifiedName)
        val fileToMove = element.containingFile
        val chosenRoot =
                sourceRoots.singleOrNull()
                ?: MoveClassesOrPackagesUtil.chooseSourceRoot(packageWrapper, sourceRoots, fileToMove.containingDirectory)
                ?: return
        val targetDirFactory = AutocreatingSingleSourceRootMoveDestination(packageWrapper, chosenRoot)
        targetDirFactory.verify(fileToMove)?.let {
            Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
            return
        }
        val targetDirectory = targetDirFactory.getTargetDirectory(fileToMove) ?: return

        RefactoringMessageUtil.checkCanCreateFile(targetDirectory, file.name)?.let {
            Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
            return
        }

        runWriteAction {
            MoveFilesOrDirectoriesUtil.doMoveFile(file, targetDirectory)
        }
    }
}