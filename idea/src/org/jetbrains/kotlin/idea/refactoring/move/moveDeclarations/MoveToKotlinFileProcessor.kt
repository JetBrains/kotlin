/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.containers.MultiMap
import com.intellij.util.text.UniqueNameGenerator
import org.jetbrains.kotlin.psi.KtFile

class MoveToKotlinFileProcessor @JvmOverloads constructor(
    project: Project,
    private val sourceFile: KtFile,
    private val targetDirectory: PsiDirectory,
    private val targetFileName: String,
    searchInComments: Boolean,
    searchInNonJavaFiles: Boolean,
    moveCallback: MoveCallback?,
    prepareSuccessfulCallback: Runnable = EmptyRunnable.INSTANCE,
    private val throwOnConflicts: Boolean = false
) : MoveFilesOrDirectoriesProcessor(
    project,
    arrayOf(sourceFile),
    targetDirectory,
    true,
    searchInComments,
    searchInNonJavaFiles,
    moveCallback,
    prepareSuccessfulCallback
) {
    override fun getCommandName() = "Move ${sourceFile.name}"

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return MoveFilesWithDeclarationsViewDescriptor(arrayOf(sourceFile), targetDirectory)
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val (conflicts, usages) = preprocessConflictUsages(refUsages)
        return showConflicts(conflicts, usages)
    }

    override fun showConflicts(conflicts: MultiMap<PsiElement, String>, usages: Array<out UsageInfo>?): Boolean {
        if (throwOnConflicts && !conflicts.isEmpty) throw RefactoringConflictsFoundException()
        return super.showConflicts(conflicts, usages)
    }

    // Assign a temporary name to file-under-move to avoid naming conflict during the refactoring
    private fun renameFileTemporarily() {
        if (targetDirectory.findFile(targetFileName) == null) return

        val temporaryName = UniqueNameGenerator.generateUniqueName("temp", "", ".kt") {
            sourceFile.containingDirectory!!.findFile(it) == null
        }
        sourceFile.name = temporaryName
    }

    override fun performRefactoring(usages: Array<UsageInfo>) {
        val needTemporaryRename = targetDirectory.findFile(targetFileName) != null
        if (needTemporaryRename) {
            renameFileTemporarily()
        }

        try {
            super.performRefactoring(usages)
        } finally {
            if (needTemporaryRename) {
                sourceFile.name = targetFileName
            }
        }
    }

    companion object {
        data class ConflictUsages(val conflicts: MultiMap<PsiElement, String>, @Suppress("ArrayInDataClass") val usages: Array<UsageInfo>)

        fun preprocessConflictUsages(refUsages: Ref<Array<UsageInfo>>): ConflictUsages {
            val usages: Array<UsageInfo> = refUsages.get()

            val (conflictUsages, usagesToProcess) = usages.partition { it is ConflictUsageInfo }

            val conflicts = MultiMap<PsiElement, String>()
            for (conflictUsage in conflictUsages) {
                conflicts.putValues(conflictUsage.element, (conflictUsage as ConflictUsageInfo).messages)
            }

            refUsages.set(usagesToProcess.toTypedArray())

            return ConflictUsages(conflicts, usages)
        }
    }
}
