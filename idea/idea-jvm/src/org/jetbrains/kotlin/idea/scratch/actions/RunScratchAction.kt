/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbService
import com.intellij.task.ProjectTaskManager
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.SequentialScratchExecutor
import org.jetbrains.kotlin.idea.scratch.getScratchFileFromSelectedEditor
import org.jetbrains.kotlin.idea.scratch.printDebugMessage
import org.jetbrains.kotlin.idea.scratch.LOG as log

class RunScratchAction : ScratchAction(
    KotlinJvmBundle.message("scratch.run.button"),
    AllIcons.Actions.Execute
) {

    init {
        KeymapManager.getInstance().activeKeymap.getShortcuts("Kotlin.RunScratch").firstOrNull()?.let {
            templatePresentation.text += " (${KeymapUtil.getShortcutText(it)})"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val scratchFile = getScratchFileFromSelectedEditor(project) ?: return

        doAction(scratchFile, false)
    }

    companion object {
        fun doAction(scratchFile: ScratchFile, isAutoRun: Boolean) {
            val isRepl = scratchFile.options.isRepl
            val executor = (if (isRepl) scratchFile.replScratchExecutor else scratchFile.compilingScratchExecutor) ?: return

            log.printDebugMessage("Run Action: isRepl = $isRepl")

            fun executeScratch() {
                try {
                    if (isAutoRun && executor is SequentialScratchExecutor) {
                        executor.executeNew()
                    } else {
                        executor.execute()
                    }
                } catch (ex: Throwable) {
                    executor.errorOccurs(KotlinJvmBundle.message("exception.occurs.during.run.scratch.action"), ex, true)
                }
            }

            val isMakeBeforeRun = scratchFile.options.isMakeBeforeRun
            log.printDebugMessage("Run Action: isMakeBeforeRun = $isMakeBeforeRun")

            val module = scratchFile.module
            log.printDebugMessage("Run Action: module = ${module?.name}")

            if (!isAutoRun && module != null && isMakeBeforeRun) {
                val project = scratchFile.project
                ProjectTaskManager.getInstance(project).build(module).onSuccess { executionResult ->
                    if (executionResult.isAborted || executionResult.hasErrors()) {
                        executor.errorOccurs(KotlinJvmBundle.message("there.were.compilation.errors.in.module.0", module.name))
                    }

                    if (DumbService.isDumb(project)) {
                        DumbService.getInstance(project).smartInvokeLater {
                            executeScratch()
                        }
                    } else {
                        executeScratch()
                    }
                }
            } else {
                executeScratch()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        e.presentation.isEnabled = !ScratchCompilationSupport.isAnyInProgress()

        if (e.presentation.isEnabled) {
            e.presentation.text = templatePresentation.text
        } else {
            e.presentation.text = KotlinJvmBundle.message("other.scratch.file.execution.is.in.progress")
        }

        val project = e.project ?: return
        val scratchFile = getScratchFileFromSelectedEditor(project) ?: return

        e.presentation.isVisible = !ScratchCompilationSupport.isInProgress(scratchFile)
    }
}