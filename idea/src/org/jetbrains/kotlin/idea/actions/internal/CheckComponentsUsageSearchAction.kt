/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import javax.swing.SwingUtilities

class CheckComponentsUsageSearchAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val selectedKotlinFiles = selectedKotlinFiles(e).toList()

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                runReadAction { process(selectedKotlinFiles, project) }
            },
            KotlinBundle.message("checking.data.classes"),
            true,
            project
        )
    }

    private fun process(files: Collection<KtFile>, project: Project) {
        val dataClasses = files.asSequence()
            .flatMap { it.declarations.asSequence() }
            .filterIsInstance<KtClass>()
            .filter { it.isData() }
            .toList()

        val progressIndicator = ProgressManager.getInstance().progressIndicator
        for ((i, dataClass) in dataClasses.withIndex()) {
            progressIndicator?.text = KotlinBundle.message("checking.data.class.0.of.1", i + 1, dataClasses.size)
            progressIndicator?.text2 = dataClass.fqName?.asString() ?: ""

            val parameter = dataClass.primaryConstructor?.valueParameters?.firstOrNull()
            if (parameter != null) {
                try {
                    var smartRefsCount = 0
                    var goldRefsCount = 0
                    ProgressManager.getInstance().runProcess(
                        {
                            ExpressionsOfTypeProcessor.mode =
                                ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART

                            smartRefsCount = ReferencesSearch.search(parameter).findAll().size

                            ExpressionsOfTypeProcessor.mode =
                                ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN

                            goldRefsCount = ReferencesSearch.search(parameter).findAll().size
                        }, EmptyProgressIndicator()
                    )

                    if (smartRefsCount != goldRefsCount) {
                        SwingUtilities.invokeLater {
                            Messages.showInfoMessage(
                                project,
                                KotlinBundle.message(
                                    "difference.found.for.data.class.0.found.1.2",
                                    dataClass.fqName?.asString().toString(),
                                    smartRefsCount,
                                    goldRefsCount
                                ),
                                KotlinBundle.message("title.error")
                            )
                        }
                        return
                    }
                } finally {
                    ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED
                }
            }

            progressIndicator?.fraction = (i + 1) / dataClasses.size.toDouble()
        }

        SwingUtilities.invokeLater {
            Messages.showInfoMessage(
                project,
                KotlinBundle.message("analyzed.0.classes.no.difference.found", dataClasses.size),
                KotlinBundle.message("title.success")
            )
        }
    }

    override fun update(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isInternal) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
        } else {
            e.presentation.isVisible = true
            e.presentation.isEnabled = true
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
