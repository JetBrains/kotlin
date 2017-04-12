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
import com.intellij.psi.PsiFile
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.containers.MultiMap
import com.intellij.util.text.UniqueNameGenerator

class MoveFilesWithDeclarationsProcessor @JvmOverloads constructor (
        project: Project,
        private val elementsToMove: List<PsiElement>,
        private val targetDirectory: PsiDirectory,
        private val targetFileName: String?,
        searchInComments: Boolean,
        searchInNonJavaFiles: Boolean,
        moveCallback: MoveCallback?,
        prepareSuccessfulCallback: Runnable = EmptyRunnable.INSTANCE
) : MoveFilesOrDirectoriesProcessor(project,
                                    elementsToMove.toTypedArray<PsiElement>(),
                                    targetDirectory,
                                    true,
                                    searchInComments,
                                    searchInNonJavaFiles,
                                    moveCallback,
                                    prepareSuccessfulCallback) {
    override fun getCommandName(): String {
        return if (targetFileName != null) "Move " + (elementsToMove.single() as PsiFile).name else "Move"
    }

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return MoveFilesWithDeclarationsViewDescriptor(elementsToMove.toTypedArray<PsiElement>(), targetDirectory)
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usages = refUsages.get()

        val (conflictUsages, usagesToProcess) = usages.partition { it is ConflictUsageInfo }

        val distinctConflictUsages = UsageViewUtil.removeDuplicatedUsages(conflictUsages.toTypedArray())
        val conflicts = MultiMap<PsiElement, String>()
        for (conflictUsage in distinctConflictUsages) {
            conflicts.putValues(conflictUsage.element, (conflictUsage as ConflictUsageInfo).messages)
        }

        refUsages.set(usagesToProcess.toTypedArray())

        return showConflicts(conflicts, usages)
    }

    // Assign a temporary name to file-under-move to avoid naming conflict during the refactoring
    private fun renameFileTemporarily() {
        if (targetFileName == null || targetDirectory.findFile(targetFileName) == null) return

        val sourceFile = elementsToMove.single() as PsiFile
        val temporaryName = UniqueNameGenerator.generateUniqueName("temp", "", ".kt") {
            sourceFile.containingDirectory!!.findFile(it) == null
        }
        sourceFile.name = temporaryName
    }

    override fun performRefactoring(usages: Array<UsageInfo>) {
        val needTemporaryRename = targetFileName != null && targetDirectory.findFile(targetFileName) != null
        if (needTemporaryRename) {
            renameFileTemporarily()
        }

        try {
            super.performRefactoring(usages)
        }
        finally {
            if (needTemporaryRename) {
                (elementsToMove.single() as PsiFile).name = targetFileName!!
            }
        }
    }
}
