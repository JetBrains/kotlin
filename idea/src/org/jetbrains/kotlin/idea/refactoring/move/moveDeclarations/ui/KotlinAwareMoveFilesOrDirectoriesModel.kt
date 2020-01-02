/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.refactoring.isInKotlinAwareSourceRoot
import org.jetbrains.kotlin.idea.refactoring.move.getOrCreateDirectory
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinAwareMoveFilesOrDirectoriesProcessor
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.InvalidPathException
import java.nio.file.Paths

class KotlinAwareMoveFilesOrDirectoriesModel(
    val project: Project,
    val elementsToMove: List<PsiFileSystemItem>,
    val targetDirectoryName: String,
    val updatePackageDirective: Boolean,
    val searchReferences: Boolean,
    val moveCallback: MoveCallback?
) : Model<KotlinAwareMoveFilesOrDirectoriesProcessor> {

    private fun checkedGetElementsToMove(selectedDirectory: PsiDirectory): List<PsiElement> {

        val preparedElementsToMove = elementsToMove
            .filterNot { it is PsiFile && it.containingDirectory == selectedDirectory }
            .sortedWith( // process Kotlin files first so that light classes are updated before changing references in Java files
                java.util.Comparator { o1, o2 ->
                    when {
                        o1 is KtElement && o2 !is KtElement -> -1
                        o1 !is KtElement && o2 is KtElement -> 1
                        else -> 0
                    }
                })

        try {
            preparedElementsToMove.forEach {
                MoveFilesOrDirectoriesUtil.checkMove(it, selectedDirectory)
                if (it is KtFile && it.isInKotlinAwareSourceRoot()) {
                    it.updatePackageDirective = updatePackageDirective
                }
            }
        } catch (e: IncorrectOperationException) {
            throw ConfigurationException(e.message)
        }

        return preparedElementsToMove
    }

    private fun checkedGetTargetDirectory(): PsiDirectory {
        try {
            return getOrCreateDirectory(targetDirectoryName, project)
        } catch (e: IncorrectOperationException) {
            throw ConfigurationException("Cannot create target directory $targetDirectoryName")
        }
    }

    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    private fun checkModel() {

        elementsToMove.firstOrNull { it !is PsiFile && it !is PsiDirectory }?.let {
            throw ConfigurationException("Unexpected element type: $it")
        }

        if (elementsToMove.isEmpty()) {
            throw ConfigurationException("There is no given files to move")
        }

        try {
            Paths.get(targetDirectoryName)
        } catch (e: InvalidPathException) {
            throw ConfigurationException("Invalid target path $targetDirectoryName")
        }

        if (DumbService.isDumb(project)) {
            throw ConfigurationException("Move refactoring is not available while indexing is in progress")
        }
    }

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): KotlinAwareMoveFilesOrDirectoriesProcessor {

        checkModel()

        val selectedDir = checkedGetTargetDirectory()

        return KotlinAwareMoveFilesOrDirectoriesProcessor(
            project,
            checkedGetElementsToMove(selectedDir),
            selectedDir,
            searchReferences = searchReferences,
            searchInComments = false,
            searchInNonJavaFiles = false,
            moveCallback = moveCallback
        )
    }
}