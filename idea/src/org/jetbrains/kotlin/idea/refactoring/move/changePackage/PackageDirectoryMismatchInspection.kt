/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.changePackage

import com.intellij.CommonBundle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.util.RefactoringMessageUtil
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.getFqNameWithImplicitPrefix
import org.jetbrains.kotlin.idea.core.packageMatchesDirectoryOrImplicit
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.refactoring.isInjectedFragment
import org.jetbrains.kotlin.idea.roots.getSuitableDestinationSourceRoots
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.packageDirectiveVisitor

class PackageDirectoryMismatchInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = packageDirectiveVisitor(fun(directive: KtPackageDirective) {
        val file = directive.containingKtFile
        if (file.isInjectedFragment || file.packageMatchesDirectoryOrImplicit()) return

        val fixes = mutableListOf<LocalQuickFix>()
        val qualifiedName = directive.qualifiedName
        val dirName = if (qualifiedName.isEmpty()) "source root" else "'${qualifiedName.replace('.', '/')}'"
        fixes += MoveFileToPackageFix(dirName)
        val fqNameByDirectory = file.getFqNameByDirectory()
        when {
            fqNameByDirectory.isRoot ->
                fixes += ChangePackageFix("source root", fqNameByDirectory)
            fqNameByDirectory.hasIdentifiersOnly() ->
                fixes += ChangePackageFix("'${fqNameByDirectory.asString()}'", fqNameByDirectory)
        }
        val fqNameWithImplicitPrefix = file.parent?.getFqNameWithImplicitPrefix()
        if (fqNameWithImplicitPrefix != null && fqNameWithImplicitPrefix != fqNameByDirectory) {
            fixes += ChangePackageFix("'${fqNameWithImplicitPrefix.asString()}'", fqNameWithImplicitPrefix)
        }

        holder.registerProblem(
            file,
            directive.textRange,
            "Package directive doesn't match file location",
            *fixes.toTypedArray()
        )
    })

    private class MoveFileToPackageFix(val dirName: String) : LocalQuickFix {
        override fun getFamilyName() = "Move file to package-matching directory"

        override fun getName() = "Move file to $dirName"

        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement as? KtFile ?: return
            val directive = file.packageDirective ?: return

            val sourceRoots = getSuitableDestinationSourceRoots(project)
            val packageWrapper = PackageWrapper(PsiManager.getInstance(project), directive.qualifiedName)
            val fileToMove = directive.containingFile
            val chosenRoot =
                sourceRoots.singleOrNull()
                    ?: MoveClassesOrPackagesUtil.chooseSourceRoot(packageWrapper, sourceRoots, fileToMove.containingDirectory)
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

    private class ChangePackageFix(val packageName: String, val packageFqName: FqName) : LocalQuickFix {
        override fun getFamilyName() = "Change file's package to match directory"

        override fun getName() = "Change file's package to $packageName"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement as? KtFile ?: return
            KotlinChangePackageRefactoring(file).run(packageFqName)
        }
    }
}