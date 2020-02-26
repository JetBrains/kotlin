/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.options.ConfigurationException
import com.intellij.psi.*
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.getOrCreateDirectory
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Paths

internal class MoveKotlinTopLevelDeclarationsModel(
    val project: Project,
    val elementsToMove: List<KtNamedDeclaration>,
    val targetPackage: String,
    val selectedPsiDirectory: PsiDirectory?,
    val fileNameInPackage: String,
    val targetFilePath: String,
    val isMoveToPackage: Boolean,
    val isSearchReferences: Boolean,
    val isSearchInComments: Boolean,
    val isSearchInNonJavaFiles: Boolean,
    val isDeleteEmptyFiles: Boolean,
    val isUpdatePackageDirective: Boolean,
    val isFullFileMove: Boolean,
    val moveCallback: MoveCallback?
) : Model<BaseRefactoringProcessor> {

    private val sourceDirectory by lazy {
        sourceFiles.singleOrNull { it.parent !== null }?.parent
            ?: throw ConfigurationException(KotlinBundle.message("text.cannot.determine.source.directory"))
    }

    private val sourceFiles = elementsToMove.map { it.containingKtFile }.distinct()

    private data class TargetDirAndDestination(val targetDir: VirtualFile?, val destination: MoveDestination)

    private fun selectPackageBasedTargetDirAndDestination(): TargetDirAndDestination {

        val targetPackageWrapper = PackageWrapper(PsiManager.getInstance(project), targetPackage)

        return if (selectedPsiDirectory === null)
            TargetDirAndDestination(null, MultipleRootsMoveDestination(targetPackageWrapper))
        else {
            TargetDirAndDestination(
                selectedPsiDirectory.virtualFile,
                AutocreatingSingleSourceRootMoveDestination(targetPackageWrapper, selectedPsiDirectory.virtualFile)
            )
        }
    }

    private fun checkTargetFileName(fileName: String) {
        if (FileTypeManager.getInstance().getFileTypeByFileName(fileName) != KotlinFileType.INSTANCE) {
            throw ConfigurationException(KotlinBundle.message("refactoring.move.non.kotlin.file"))
        }
    }

    private fun getFilesExistingInTargetDir(
        targetFileName: String?,
        targetDirectory: PsiDirectory?
    ): List<PsiFile> {
        targetDirectory ?: return emptyList()

        val fileNames = targetFileName?.let { listOf(it) } ?: sourceFiles.map { it.name }

        return fileNames
            .distinct()
            .mapNotNull { targetDirectory.findFile(it) }
    }

    private fun selectMoveTargetToPackage(): KotlinMoveTarget {

        if (sourceFiles.size > 1) throw ConfigurationException(KotlinBundle.message("text.cannot.move.from.multiple.files"))

        checkTargetFileName(fileNameInPackage)

        val (targetDir, moveDestination) = selectPackageBasedTargetDirAndDestination()

        val targetDirectory = moveDestination.getTargetIfExists(sourceDirectory)

        val filesExistingInTargetDir = getFilesExistingInTargetDir(fileNameInPackage, targetDirectory)
        if (filesExistingInTargetDir.isNotEmpty()) {
            if (filesExistingInTargetDir.size > 1) {
                val filePathsToReport = KotlinBundle.message(
                    "text.move.refactoring.conflict.0",
                    filesExistingInTargetDir.joinToString("\n") { it.virtualFile.path }
                )
                throw ConfigurationException(filePathsToReport)
            }

            (filesExistingInTargetDir[0] as? KtFile)?.let {
                return KotlinMoveTargetForExistingElement(it)
            }
        }

        // All source files must be in the same directory
        return KotlinMoveTargetForDeferredFile(
            FqName(targetPackage),
            moveDestination.getTargetIfExists(sourceFiles[0]),
            targetDir
        ) { getOrCreateKotlinFile(fileNameInPackage, moveDestination.getTargetDirectory(it)) }
    }

    private fun selectMoveTargetToFile(): KotlinMoveTarget {

        try {
            Paths.get(targetFilePath)
        } catch (e: InvalidPathException) {
            throw ConfigurationException(KotlinBundle.message("text.invalid.target.path.0", targetFilePath))
        }

        val targetFile = File(targetFilePath)
        checkTargetFileName(targetFile.name)

        val jetFile = targetFile.toPsiFile(project) as? KtFile
        if (jetFile !== null) {
            if (sourceFiles.size == 1 && sourceFiles.contains(jetFile)) {
                throw ConfigurationException(KotlinBundle.message("text.cannot.move.to.original.file"))
            }
            return KotlinMoveTargetForExistingElement(jetFile)
        }

        val targetDirPath = targetFile.toPath().parent
        val projectBasePath = project.basePath ?: throw ConfigurationException(KotlinBundle.message("text.cannot.move.for.current.project"))
        if (targetDirPath === null || !targetDirPath.startsWith(projectBasePath)) {
            throw ConfigurationException(
                KotlinBundle.message("text.incorrect.target.path.directory.0.does.not.belong.to.current.project", targetDirPath)
            )
        }

        val absoluteTargetDirPath = targetDirPath.toString()
        val psiDirectory: PsiDirectory
        try {
            psiDirectory = getOrCreateDirectory(absoluteTargetDirPath, project)
        } catch (e: IncorrectOperationException) {
            throw ConfigurationException(KotlinBundle.message("text.failed.to.create.parent.directory.0", absoluteTargetDirPath))
        }

        val targetPackageFqName = sourceFiles.singleOrNull()?.packageFqName
            ?: JavaDirectoryService.getInstance().getPackage(psiDirectory)?.let { FqName(it.qualifiedName) }
            ?: throw ConfigurationException(KotlinBundle.message("text.cannot.find.package.corresponding.to.0", absoluteTargetDirPath))

        val finalTargetPackageFqName = targetPackageFqName.asString()
        return KotlinMoveTargetForDeferredFile(
            targetPackageFqName,
            psiDirectory,
            targetFile = null
        ) { getOrCreateKotlinFile(targetFile.name, psiDirectory, finalTargetPackageFqName) }
    }

    private fun selectMoveTarget() =
        if (isMoveToPackage) selectMoveTargetToPackage() else selectMoveTargetToFile()

    private fun verifyBeforeRun(target: KotlinMoveTarget) {

        if (elementsToMove.isEmpty()) throw ConfigurationException(KotlinBundle.message("text.at.least.one.file.must.be.selected"))

        for (element in elementsToMove) {
            target.verify(element.containingFile)?.let { throw ConfigurationException(it) }
        }

        if (isMoveToPackage) {
            if (targetPackage.isNotEmpty() && !PsiNameHelper.getInstance(project).isQualifiedName(targetPackage)) {
                throw ConfigurationException(KotlinBundle.message("text.0.is.invalid.destination.package", targetPackage))
            }
        } else {
            val targetFile = File(targetFilePath).toPsiFile(project)
            if (targetFile !== null && targetFile !is KtFile) {
                throw ConfigurationException(KotlinBundle.message("refactoring.move.non.kotlin.file"))
            }
        }

        if (sourceFiles.size == 1 && fileNameInPackage.isEmpty()) {
            throw ConfigurationException(KotlinBundle.message("text.file.name.cannot.be.empty"))
        }
    }

    private val verifiedMoveTarget get() = selectMoveTarget().also { verifyBeforeRun(it) }

    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): BaseRefactoringProcessor {

        val target = verifiedMoveTarget

        if (isFullFileMove && isMoveToPackage) {
            val (_, moveDestination) = selectPackageBasedTargetDirAndDestination()

            val targetDir = moveDestination.getTargetIfExists(sourceDirectory)
            val targetFileName = if (sourceFiles.size > 1) null else fileNameInPackage

            val filesExistingInTargetDir = getFilesExistingInTargetDir(targetFileName, targetDir)

            if (filesExistingInTargetDir.isEmpty() ||
                (filesExistingInTargetDir.size == 1 && sourceFiles.contains(filesExistingInTargetDir[0]))
            ) {
                val targetDirectory = project.executeCommand(RefactoringBundle.message("move.title"), null) {
                    runWriteAction<PsiDirectory> {
                        moveDestination.getTargetDirectory(sourceDirectory)
                    }
                }

                sourceFiles.forEach { it.updatePackageDirective = isUpdatePackageDirective }

                return if (sourceFiles.size == 1 && targetFileName !== null)
                    MoveToKotlinFileProcessor(
                        project,
                        sourceFiles[0],
                        targetDirectory,
                        targetFileName,
                        searchInComments = isSearchInComments,
                        searchInNonJavaFiles = isSearchInNonJavaFiles,
                        moveCallback = moveCallback,
                        throwOnConflicts = throwOnConflicts
                    )
                else
                    KotlinAwareMoveFilesOrDirectoriesProcessor(
                        project,
                        sourceFiles,
                        targetDirectory,
                        isSearchReferences,
                        searchInComments = isSearchInComments,
                        searchInNonJavaFiles = isSearchInNonJavaFiles,
                        moveCallback = moveCallback,
                        throwOnConflicts = throwOnConflicts
                    )
            }
        }

        val options = MoveDeclarationsDescriptor(
            project,
            MoveSource(elementsToMove),
            target,
            MoveDeclarationsDelegate.TopLevel,
            isSearchInComments,
            isSearchInNonJavaFiles,
            deleteSourceFiles = isFullFileMove && isDeleteEmptyFiles,
            moveCallback = moveCallback,
            openInEditor = false,
            allElementsToMove = null,
            analyzeConflicts = true,
            searchReferences = isSearchReferences
        )
        return MoveKotlinDeclarationsProcessor(options, Mover.Default, throwOnConflicts)
    }
}