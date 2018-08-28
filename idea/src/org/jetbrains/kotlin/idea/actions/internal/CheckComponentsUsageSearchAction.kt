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

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor
import org.jetbrains.kotlin.idea.util.application.progressIndicatorNullable
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import javax.swing.SwingUtilities

class CheckComponentsUsageSearchAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val selectedFiles = selectedKotlinFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)!!

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    runReadAction { process(selectedFiles, project) }
                },
                "Checking Data Classes",
                true,
                project)
    }

    private fun process(files: Collection<KtFile>, project: Project) {
        val dataClasses = files.asSequence()
                .flatMap { it.declarations.asSequence() }
                .filterIsInstance<KtClass>()
                .filter { it.isData() }
                .toList()

        val progressIndicator = ProgressManager.getInstance().progressIndicatorNullable
        for ((i, dataClass) in dataClasses.withIndex()) {
            progressIndicator?.text = "Checking data class ${i + 1} of ${dataClasses.size}..."
            progressIndicator?.text2 = dataClass.fqName?.asString() ?: ""

            val parameter = dataClass.primaryConstructor?.valueParameters?.firstOrNull()
            if (parameter != null) {
                try {
                    var smartRefsCount = 0
                    var goldRefsCount = 0
                    ProgressManager.getInstance().runProcess({
                        ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART

                        smartRefsCount = ReferencesSearch.search(parameter).findAll().size

                        ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN

                        goldRefsCount = ReferencesSearch.search(parameter).findAll().size
                    }, EmptyProgressIndicator())

                    if (smartRefsCount != goldRefsCount) {
                        SwingUtilities.invokeLater {
                            Messages.showInfoMessage(project, "Difference found for data class ${dataClass.fqName?.asString()}. Found $smartRefsCount usage(s) but $goldRefsCount expected", "Error")
                        }
                        return
                    }
                }
                finally {
                    ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED
                }
            }

            progressIndicator?.fraction = (i + 1) / dataClasses.size.toDouble()
        }

        SwingUtilities.invokeLater {
            Messages.showInfoMessage(project, "Analyzed ${dataClasses.size} classes. No difference found.", "Success")
        }
    }

    override fun update(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isInternal) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
        }
        else {
            e.presentation.isVisible = true
            e.presentation.isEnabled = selectedKotlinFiles(e).any()
        }
    }

    private fun selectedKotlinFiles(e: AnActionEvent): Sequence<KtFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return sequenceOf()
        return allKotlinFiles(virtualFiles, project)
    }

    private fun allKotlinFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<KtFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .asSequence()
                .mapNotNull { manager.findFile(it) as? KtFile }
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }
}
