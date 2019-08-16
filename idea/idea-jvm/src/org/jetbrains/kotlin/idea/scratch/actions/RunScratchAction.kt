/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbService
import com.intellij.task.ProjectTaskManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.idea.scratch.printDebugMessage
import org.jetbrains.kotlin.idea.scratch.LOG as log

class RunScratchAction : ScratchAction(
    KotlinBundle.message("scratch.run.button"),
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
                    executor.errorOccurs("Exception occurs during Run Scratch Action", ex, true)
                }
            }

            val isMakeBeforeRun = scratchFile.options.isMakeBeforeRun
            log.printDebugMessage("Run Action: isMakeBeforeRun = $isMakeBeforeRun")

            val module = scratchFile.module
            log.printDebugMessage("Run Action: module = ${module?.name}")

            if (!isAutoRun && module != null && isMakeBeforeRun) {
                val project = scratchFile.project
                ProjectTaskManager.getInstance(project).build(arrayOf(module)) { result ->
                    if (result.isAborted || result.errors > 0) {
                        executor.errorOccurs("There were compilation errors in module ${module.name}")
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
            e.presentation.text = "Other Scratch file execution is in progress"
        }

        val project = e.project ?: return
        val scratchFile = getScratchFileFromSelectedEditor(project) ?: return

        e.presentation.isVisible = !ScratchCompilationSupport.isInProgress(scratchFile)
    }
}