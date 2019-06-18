/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.ide.util.DirectoryChooser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.RecentsManager
import com.intellij.util.Function
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile


fun getFilesExistingInTargetDir(
    sourceFiles: List<KtFile>,
    targetFileName: String?,
    targetDirectory: PsiDirectory?
): List<PsiFile> {
    if (targetDirectory == null) return emptyList()

    val fileNames = if (targetFileName != null)
        listOf(targetFileName)
    else
        sourceFiles.map { jetFile -> jetFile.name }

    return fileNames.map { s -> targetDirectory.findFile(s) }.filterNotNull()
}

fun selectPackageBasedTargetDirAndDestination(
    myProject: Project,
    packageName: String,
    askIfDoesNotExist: Boolean,
    recentsKey: String,
    initialTargetDirectory: PsiDirectory?,
    destinationFolderCB: ComboboxWithBrowseButton?
): Pair<VirtualFile, out MoveDestination>? {
    RecentsManager.getInstance(myProject).registerRecentEntry(recentsKey, packageName)
    val targetPackage = PackageWrapper(PsiManager.getInstance(myProject), packageName)
    if (!targetPackage.exists() && askIfDoesNotExist) {
        val ret = Messages.showYesNoDialog(
            myProject, RefactoringBundle.message("package.does.not.exist", packageName),
            RefactoringBundle.message("move.title"), Messages.getQuestionIcon()
        )
        if (ret != Messages.YES) return null
    }

    val selectedItem = destinationFolderCB?.comboBox?.selectedItem as? DirectoryChooser.ItemWrapper
    var selectedPsiDirectory: PsiDirectory? = selectedItem?.directory
    if (selectedPsiDirectory == null) {
        if (initialTargetDirectory != null) {
            selectedPsiDirectory = initialTargetDirectory
        } else {
            return Pair.create(null, MultipleRootsMoveDestination(targetPackage))
        }
    }

    val targetDirectory = selectedPsiDirectory.virtualFile
    return Pair.create(targetDirectory, AutocreatingSingleSourceRootMoveDestination(targetPackage, targetDirectory))
}

fun getSourceDirectory(sourceFiles: Collection<KtFile>): PsiDirectory = sourceFiles.map { jetFile ->
    jetFile.parent
}.distinct().single()!!

fun getPackageMoveTarget(
    sourceFiles: List<KtFile>,
    packageName: String,
    myProject: Project,
    askIfDoesNotExist: Boolean,
    recentsKey: String,
    initialTargetDirectory: PsiDirectory?,
    destinationFolderCB: ComboboxWithBrowseButton?,
    targetFileName: String?
): KotlinMoveTarget? {

    val targetDirWithMoveDestination = selectPackageBasedTargetDirAndDestination(
        myProject,
        packageName,
        askIfDoesNotExist,
        recentsKey,
        initialTargetDirectory,
        destinationFolderCB
    ) ?: return null

    val targetDir = targetDirWithMoveDestination.getFirst()
    val moveDestination = targetDirWithMoveDestination.getSecond()

    val targetDirectory = moveDestination.getTargetIfExists(getSourceDirectory(sourceFiles))

    val filesExistingInTargetDir = getFilesExistingInTargetDir(sourceFiles, targetFileName, targetDirectory)
    if (!filesExistingInTargetDir.isEmpty()) {
        if (filesExistingInTargetDir.size > 1) {
            val filePathsToReport = StringUtil.join<PsiFile>(
                filesExistingInTargetDir,
                Function<PsiFile, String> { file -> file.virtualFile.path },
                "\n"
            )
            Messages.showErrorDialog(
                myProject,
                "Cannot perform refactoring since the following files already exist:\n\n$filePathsToReport",
                RefactoringBundle.message("move.title")
            )
            return null
        }

        val targetFile = filesExistingInTargetDir.get(0)

        if (!sourceFiles.contains(targetFile)) {
            val question = String.format(
                "File '%s' already exists. Do you want to move selected declarations to this file?",
                targetFile.getVirtualFile().getPath()
            )
            val ret = Messages.showYesNoDialog(
                myProject, question, RefactoringBundle.message("move.title"),
                Messages.getQuestionIcon()
            )
            if (ret != Messages.YES) return null
        }

        if (targetFile is KtFile) {
            return KotlinMoveTargetForExistingElement(targetFile as KtFile)
        }
    }

    return KotlinMoveTargetForDeferredFile(
        FqName(packageName),
        targetFileName,
        moveDestination.getTargetIfExists(sourceFiles[0]),
        targetDir,
        module = sourceFiles.first().module
    ) { originalFile ->
        getOrCreateKotlinFile(
            if (targetFileName != null) targetFileName else originalFile.name,
            moveDestination.getTargetDirectory(originalFile)
        )
    }
}

fun doMoveToPackage(
    sourceFiles: List<KtFile>,
    myProject: Project,
    packageName: String,
    askIfDoesNotExist: Boolean,
    recentsKey: String,
    updatePackageDirective: Boolean,
    searchReferences: Boolean,
    searchInComments: Boolean,
    searchInNonJavaFiles: Boolean,
    moveCallback: MoveCallback?,
    initialTargetDirectory: PsiDirectory?,
    destinationFolderCB: ComboboxWithBrowseButton?,
    targetFileName: String?,
    invokeRefactoring: (BaseRefactoringProcessor) -> Unit
) {
    val sourceRootWithMoveDestination = selectPackageBasedTargetDirAndDestination(
        myProject,
        packageName,
        askIfDoesNotExist,
        recentsKey,
        initialTargetDirectory,
        destinationFolderCB
    )

    val moveDestination = sourceRootWithMoveDestination!!.second

    val sourceDirectory = getSourceDirectory(sourceFiles)

    val targetDir = moveDestination.getTargetIfExists(sourceDirectory)
    val filesExistingInTargetDir = getFilesExistingInTargetDir(sourceFiles, targetFileName, targetDir)
    if (filesExistingInTargetDir.isEmpty() || filesExistingInTargetDir.size == 1 && sourceFiles.contains(filesExistingInTargetDir[0])) {
        val targetDirectory = runWriteAction<PsiDirectory> { moveDestination.getTargetDirectory(sourceDirectory) }

        for (sourceFile in sourceFiles) {
            sourceFile.updatePackageDirective = updatePackageDirective
        }

        val processor: BaseRefactoringProcessor
        processor = if (sourceFiles.size == 1 && targetFileName != null)
            MoveToKotlinFileProcessor(
                myProject,
                sourceFiles.single(),
                targetDirectory,
                targetFileName,
                searchInComments,
                searchInNonJavaFiles,
                moveCallback
            )
        else
            KotlinAwareMoveFilesOrDirectoriesProcessor(
                myProject,
                sourceFiles,
                targetDirectory,
                searchReferences,
                searchInComments,
                searchInNonJavaFiles,
                moveCallback
            )

        invokeRefactoring(processor)

        return
    }
}