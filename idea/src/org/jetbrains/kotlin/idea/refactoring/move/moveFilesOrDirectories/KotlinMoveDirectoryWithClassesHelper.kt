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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.move.moveClassesOrPackages.MoveDirectoryWithClassesHelper
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.Function
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.refactoring.invokeOnceOnCommandFinish
import org.jetbrains.kotlin.idea.refactoring.move.KotlinMoveUsage
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinDirectoryMoveTarget
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveConflictChecker
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsProcessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

class KotlinMoveDirectoryWithClassesHelper : MoveDirectoryWithClassesHelper() {
    private data class FileUsagesWrapper(
            val psiFile: KtFile,
            val usages: List<UsageInfo>,
            val moveDeclarationsProcessor: MoveKotlinDeclarationsProcessor?
    ) : UsageInfo(psiFile)

    private class MoveContext(
            val newParent: PsiDirectory,
            val moveDeclarationsProcessor: MoveKotlinDeclarationsProcessor?
    )

    private val fileHandler = MoveKotlinFileHandler()

    private var fileToMoveContext: MutableMap<PsiFile, MoveContext>? = null

    private fun getOrCreateMoveContextMap(): MutableMap<PsiFile, MoveContext> {
        return fileToMoveContext ?: HashMap<PsiFile, MoveContext>().apply {
            fileToMoveContext = this
            invokeOnceOnCommandFinish { fileToMoveContext = null }
        }
    }

    override fun findUsages(
            filesToMove: MutableCollection<PsiFile>,
            directoriesToMove: Array<out PsiDirectory>,
            result: MutableCollection<UsageInfo>,
            searchInComments: Boolean,
            searchInNonJavaFiles: Boolean,
            project: Project) {
        filesToMove
                .filterIsInstance<KtFile>()
                .mapTo(result) { FileUsagesWrapper(it, fileHandler.findUsages(it, null, searchInComments, searchInNonJavaFiles), null) }
    }

    override fun preprocessUsages(
            project: Project,
            files: MutableSet<PsiFile>,
            infos: Array<UsageInfo>,
            directory: PsiDirectory?,
            conflicts: MultiMap<PsiElement, String>
    ) {
        val psiPackage = directory?.getPackage() ?: return
        val moveTarget = KotlinDirectoryMoveTarget(FqName(psiPackage.qualifiedName), directory)
        for ((index, usageInfo) in infos.withIndex()) {
            if (usageInfo !is FileUsagesWrapper) continue

            val elementsToMove = usageInfo.psiFile.declarations
            if (elementsToMove.isEmpty()) continue

            val (internalUsages, externalUsages) = usageInfo.usages.partition { it is KotlinMoveUsage && it.isInternal }
            val internalUsageSet = internalUsages.toMutableSet()
            val externalUsageSet = externalUsages.toMutableSet()

            val conflictChecker = MoveConflictChecker(
                    project,
                    elementsToMove,
                    moveTarget,
                    elementsToMove.first(),
                    allElementsToMove = files
            )
            conflictChecker.checkAllConflicts(externalUsageSet, internalUsageSet, conflicts)
            if (externalUsageSet.size != externalUsages.size || internalUsageSet.size != internalUsages.size) {
                infos[index] = usageInfo.copy(usages = (externalUsageSet + internalUsageSet).toList())
            }
        }
    }

    override fun beforeMove(psiFile: PsiFile) {

    }

    // Actual move logic is implemented in postProcessUsages since usages are not available here
    override fun move(
            file: PsiFile,
            moveDestination: PsiDirectory,
            oldToNewElementsMapping: MutableMap<PsiElement, PsiElement>,
            movedFiles: MutableList<PsiFile>,
            listener: RefactoringElementListener?
    ): Boolean {
        if (file !is KtFile) return false

        val moveDeclarationsProcessor = fileHandler.initMoveProcessor(file, moveDestination)
        val moveContextMap = getOrCreateMoveContextMap()
        moveContextMap[file] = MoveContext(moveDestination, moveDeclarationsProcessor)
        if (moveDeclarationsProcessor != null) {
            moveDestination.getPackage()?.let { newPackage ->
                file.packageDirective?.fqName = FqName(newPackage.qualifiedName).quoteIfNeeded()
            }
        }
        return true
    }

    override fun afterMove(newElement: PsiElement) {

    }

    override fun postProcessUsages(usages: Array<out UsageInfo>, newDirMapper: Function<PsiDirectory, PsiDirectory>) {
        val fileToMoveContext = fileToMoveContext ?: return
        try {
            val usagesToProcess = ArrayList<FileUsagesWrapper>()
            usages
                .filterIsInstance<FileUsagesWrapper>()
                .forEach body@ {
                    val file = it.psiFile
                    val moveContext = fileToMoveContext[file] ?: return@body

                    MoveFilesOrDirectoriesUtil.doMoveFile(file, moveContext.newParent)

                    val moveDeclarationsProcessor = moveContext.moveDeclarationsProcessor ?: return@body
                    val movedFile = moveContext.newParent.findFile(file.name) ?: return@body

                    usagesToProcess += FileUsagesWrapper(movedFile as KtFile, it.usages, moveDeclarationsProcessor)
                }
            usagesToProcess.forEach { fileHandler.retargetUsages(it.usages, it.moveDeclarationsProcessor!!) }
        }
        finally {
            this.fileToMoveContext = null
        }
    }
}