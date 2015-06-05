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
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.util.RefactoringMessageUtil
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory

public class MoveFileToPackageMatchingDirectoryIntention : JetSelfTargetingOffsetIndependentIntention<JetPackageDirective>(
        javaClass(), "", "Move file to package-matching directory"
) {
    override fun isApplicableTo(element: JetPackageDirective): Boolean {
        if (element.getContainingJetFile().packageMatchesDirectory()) return false

        val qualifiedName = element.getQualifiedName()
        val dirName = if (qualifiedName.isEmpty()) "source root" else "'${qualifiedName.replace('.', '/')}'"
        setText("Move file to $dirName")
        return true
    }

    override fun applyTo(element: JetPackageDirective, editor: Editor) {
        val file = element.getContainingJetFile()
        val project = file.getProject()
        val targetDirectory = MoveClassesOrPackagesUtil.chooseDestinationPackage(
                project,
                element.getQualifiedName(),
                file.getContainingDirectory()
        ) ?: return

        RefactoringMessageUtil.checkCanCreateFile(targetDirectory, file.getName())?.let {
            Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
            return
        }

        MoveFilesOrDirectoriesUtil.doMoveFile(file, targetDirectory)
    }
}