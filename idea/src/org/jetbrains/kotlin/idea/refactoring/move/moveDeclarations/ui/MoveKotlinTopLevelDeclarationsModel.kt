/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.options.ConfigurationException
import com.intellij.psi.*
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.getOrCreateDirectory
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
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

    private inline fun <T, K> Set<T>.mapToSingleOrNull(transform: (T) -> K?): K? =
        mapTo(mutableSetOf(), transform).singleOrNull()

    private fun checkedGetSourceDirectory() =
        sourceFiles.mapToSingleOrNull { it.parent } ?: throw ConfigurationException("Can't determine sources directory")

    private val sourceFiles: Set<KtFile> = elementsToMove.mapTo(mutableSetOf()) { it.containingKtFile }

    private val singleSourceFileMode = sourceFiles.size == 1

    private fun selectPackageBasedDestination(): MoveDestination {

        val targetPackageWrapper = PackageWrapper(PsiManager.getInstance(project), targetPackage)

        return if (selectedPsiDirectory == null)
            MultipleRootsMoveDestination(targetPackageWrapper)
        else
            AutocreatingSingleSourceRootMoveDestination(targetPackageWrapper, selectedPsiDirectory.virtualFile)
    }

    private fun checkTargetFileName(fileName: String) {
        if (FileTypeManager.getInstance().getFileTypeByFileName(fileName) != KotlinFileType.INSTANCE) {
            throw ConfigurationException(KotlinRefactoringBundle.message("refactoring.move.non.kotlin.file"))
        }
    }

    private fun getFilesExistingInTargetDirectory(
        targetFileName: String?,
        targetDirectory: PsiDirectory
    ): Set<PsiFile> {
        return if (targetFileName != null) {
            targetDirectory.findFile(targetFileName)?.let { setOf(it) }.orEmpty()
        } else {
            sourceFiles.mapNotNullTo(mutableSetOf()) { targetDirectory.findFile(it.name) }
        }
    }

    private fun tryMoveToPackageForExistingDirectory(targetFileName: String?, targetDirectory: PsiDirectory): KotlinMoveTarget? {

        val filesExistingInTargetDir = getFilesExistingInTargetDirectory(targetFileName, targetDirectory)

        if (filesExistingInTargetDir.isEmpty()) return null

        if (singleSourceFileMode) {
            val singeTargetFile = filesExistingInTargetDir.single() as? KtFile
            if (singeTargetFile != null) {
                return KotlinMoveTargetForExistingElement(singeTargetFile)
            }
        } else {
            val filePathsToReport = filesExistingInTargetDir.joinToString(
                separator = "\n",
                prefix = "Cannot perform refactoring since the following files already exist:\n\n"
            ) { it.virtualFile.path }
            throw ConfigurationException(filePathsToReport)
        }

        return null
    }

    private fun selectMoveTargetToPackage(): KotlinMoveTarget {
        require(sourceFiles.isNotEmpty())

        val moveDestination = selectPackageBasedDestination()
        val targetDirectory: PsiDirectory? = moveDestination.getTargetIfExists(checkedGetSourceDirectory())

        val targetFileName = if (singleSourceFileMode) fileNameInPackage.also(::checkTargetFileName) else null

        if (targetDirectory != null) {
            tryMoveToPackageForExistingDirectory(targetFileName, targetDirectory)?.let { return it }
        }

        return KotlinMoveTargetForDeferredFile(
            FqName(targetPackage),
            targetDirectory
        ) {
            val deferredFileName = if (singleSourceFileMode) fileNameInPackage else it.name
            val deferredFileDirectory = moveDestination.getTargetDirectory(it)
            getOrCreateKotlinFile(deferredFileName, deferredFileDirectory)
        }
    }

    private fun selectMoveTargetToFile(): KotlinMoveTarget {

        val targetFile = try {
            Paths.get(targetFilePath).toFile()
        } catch (e: InvalidPathException) {
            throw ConfigurationException("Invalid target path $targetFilePath")
        }

        checkTargetFileName(targetFile.name)

        val jetFile = targetFile.toPsiFile(project) as? KtFile
        if (jetFile != null) {
            if (sourceFiles.singleOrNull() == jetFile) {
                throw ConfigurationException("Can't move to the original file")
            }
            return KotlinMoveTargetForExistingElement(jetFile)
        }

        val targetDirectoryPath = targetFile.toPath().parent
            ?: throw ConfigurationException("Incorrect target path. Directory is not specified.")

        val projectBasePath = project.basePath
            ?: throw ConfigurationException("Can't move for current project")

        if (!targetDirectoryPath.startsWith(projectBasePath)) {
            throw ConfigurationException("Incorrect target path. Directory $targetDirectoryPath does not belong to current project.")
        }

        val psiDirectory = targetDirectoryPath.toFile().toPsiDirectory(project)

        val targetPackageFqName = sourceFiles.singleOrNull()?.packageFqName
            ?: psiDirectory?.getPackage()?.let { FqName(it.qualifiedName) }
            ?: throw ConfigurationException("Could not find package corresponding to $targetDirectoryPath")

        val targetDirectoryPathString = targetDirectoryPath.toString()
        val finalTargetPackageFqName = targetPackageFqName.asString()

        return KotlinMoveTargetForDeferredFile(
            targetPackageFqName,
            psiDirectory,
            targetFile = null
        ) {
            val actualPsiDirectory = psiDirectory ?: getOrCreateDirectory(targetDirectoryPathString, project)
            getOrCreateKotlinFile(targetFile.name, actualPsiDirectory, finalTargetPackageFqName)
        }
    }

    private fun selectMoveTarget() =
        if (isMoveToPackage) selectMoveTargetToPackage() else selectMoveTargetToFile()

    private fun verifyBeforeRun() {

        if (elementsToMove.isEmpty()) throw ConfigurationException("At least one member must be selected")
        if (sourceFiles.isEmpty()) throw ConfigurationException("None elements were selected")
        if (singleSourceFileMode && fileNameInPackage.isBlank()) throw ConfigurationException("File name may not be empty")

        if (isMoveToPackage) {
            if (targetPackage.isNotEmpty() && !PsiNameHelper.getInstance(project).isQualifiedName(targetPackage)) {
                throw ConfigurationException("\'$targetPackage\' is invalid destination package name")
            }
        } else {
            val targetFile = File(targetFilePath).toPsiFile(project)
            if (targetFile != null && targetFile !is KtFile) {
                throw ConfigurationException(KotlinRefactoringBundle.message("refactoring.move.non.kotlin.file"))
            }
        }
    }

    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): BaseRefactoringProcessor {

        verifyBeforeRun()

        if (isFullFileMove && isMoveToPackage) {
            tryMoveFile(throwOnConflicts)?.let { return it }
        }

        return moveDeclaration(throwOnConflicts)
    }

    private fun tryMoveFile(throwOnConflicts: Boolean): BaseRefactoringProcessor? {

        val targetFileName = if (sourceFiles.size > 1) null else fileNameInPackage
        if (targetFileName != null) checkTargetFileName(targetFileName)

        val moveDestination = selectPackageBasedDestination()
        val targetDirectory = moveDestination.getTargetIfExists(checkedGetSourceDirectory()) ?: return null

        val filesExistingInTargetDir = getFilesExistingInTargetDirectory(targetFileName, targetDirectory)

        val moveAsFile = filesExistingInTargetDir.isEmpty() ||
                filesExistingInTargetDir.singleOrNull()?.let { sourceFiles.contains(it) } == true

        if (!moveAsFile) return null

        sourceFiles.forEach { it.updatePackageDirective = isUpdatePackageDirective }

        return if (targetFileName != null)
            MoveToKotlinFileProcessor(
                project,
                sourceFiles.first(),
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
                sourceFiles.toList(),
                targetDirectory,
                isSearchReferences,
                searchInComments = isSearchInComments,
                searchInNonJavaFiles = isSearchInNonJavaFiles,
                moveCallback = moveCallback,
                throwOnConflicts = throwOnConflicts
            )
    }

    private fun moveDeclaration(throwOnConflicts: Boolean): BaseRefactoringProcessor {
        val target = selectMoveTarget()
        for (element in elementsToMove) {
            target.verify(element.containingFile)?.let { throw ConfigurationException(it) }
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