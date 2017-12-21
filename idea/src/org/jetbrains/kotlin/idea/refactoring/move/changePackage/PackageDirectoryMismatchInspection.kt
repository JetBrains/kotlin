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
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.util.RefactoringMessageUtil
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.refactoring.isInjectedFragment
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtVisitorVoid

class PackageDirectoryMismatchInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitPackageDirective(directive: KtPackageDirective) {
                    super.visitPackageDirective(directive)
                    if (directive.text.isEmpty()) return

                    val file = directive.containingKtFile
                    if (file.isInjectedFragment || file.packageMatchesDirectory()) return

                    val fixes = mutableListOf<LocalQuickFix>()
                    val qualifiedName = directive.qualifiedName
                    val dirName = if (qualifiedName.isEmpty()) "source root" else "'${qualifiedName.replace('.', '/')}'"
                    fixes += MoveFileToPackageFix(dirName)
                    val fqNameByDirectory = file.getFqNameByDirectory()
                    if (fqNameByDirectory.hasIdentifiersOnly()) {
                        fixes += ChangePackageFix(fqNameByDirectory)
                    }

                    holder.registerProblem(
                            directive,
                            "Package directive doesn't match file location",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            *fixes.toTypedArray()
                    )
                }
            }

    private class MoveFileToPackageFix(val dirName: String) : LocalQuickFix {
        override fun getFamilyName() = "Move file to package-matching directory"

        override fun getName() = "Move file to $dirName"

        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val directive = descriptor.psiElement as? KtPackageDirective ?: return
            val file = directive.containingKtFile

            val sourceRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(project)
            val packageWrapper = PackageWrapper(PsiManager.getInstance(project), directive.qualifiedName)
            val fileToMove = directive.containingFile
            val chosenRoot =
                    sourceRoots.singleOrNull()
                    ?: MoveClassesOrPackagesUtil.chooseSourceRoot(packageWrapper, sourceRoots,
                                                                  fileToMove.containingDirectory)
                    ?: return
            val targetDirFactory = AutocreatingSingleSourceRootMoveDestination(packageWrapper, chosenRoot)
            targetDirFactory.verify(fileToMove)?.let {
                Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
                return
            }
            val targetDirectory = runWriteAction {
                targetDirFactory.getTargetDirectory(fileToMove)
            } ?: return

            RefactoringMessageUtil.checkCanCreateFile(targetDirectory, file.name)?.let {
                Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
                return
            }

            runWriteAction {
                MoveFilesOrDirectoriesUtil.doMoveFile(file, targetDirectory)
            }
        }
    }

    private class ChangePackageFix(val fqNameByDirectory: FqName) : LocalQuickFix {
        override fun getFamilyName() = "Change file's package to match directory"

        override fun getName() = "Change file's package to '${fqNameByDirectory.asString()}'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val directive = descriptor.psiElement as? KtPackageDirective ?: return
            val file = directive.containingKtFile
            val newFqName = file.getFqNameByDirectory()
            if (!newFqName.hasIdentifiersOnly()) return
            KotlinChangePackageRefactoring(file).run(newFqName)

        }
    }
}