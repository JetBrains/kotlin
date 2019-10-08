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

package org.jetbrains.kotlin.idea.scratch.ui


import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.ScratchFileAutoRunner
import org.jetbrains.kotlin.idea.scratch.actions.ClearScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.StopScratchAction
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputHandlerAdapter

class ScratchTopPanel(val scratchFile: ScratchFile) {
    private val moduleChooserAction: ModulesComboBoxAction = ModulesComboBoxAction(scratchFile)
    val actionsToolbar: ActionToolbar

    init {
        setupTopPanelUpdateHandlers()

        val toolbarGroup = DefaultActionGroup().apply {
            add(RunScratchAction())
            add(StopScratchAction())
            addSeparator()
            add(ClearScratchAction())
            addSeparator()
            add(moduleChooserAction)
            add(IsMakeBeforeRunAction())
            addSeparator()
            add(IsInteractiveCheckboxAction())
            addSeparator()
            add(IsReplCheckboxAction())
        }

        actionsToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true)
    }

    private fun setupTopPanelUpdateHandlers() {
        scratchFile.addModuleListener { _, _ -> updateToolbar() }

        val toolbarHandler = createUpdateToolbarHandler()
        scratchFile.replScratchExecutor?.addOutputHandler(toolbarHandler)
        scratchFile.compilingScratchExecutor?.addOutputHandler(toolbarHandler)
    }

    private fun createUpdateToolbarHandler(): ScratchOutputHandlerAdapter {
        return object : ScratchOutputHandlerAdapter() {
            override fun onStart(file: ScratchFile) {
                updateToolbar()
            }

            override fun onFinish(file: ScratchFile) {
                updateToolbar()
            }
        }
    }

    private fun updateToolbar() {
        ApplicationManager.getApplication().invokeLater {
            actionsToolbar.updateActionsImmediately()
        }
    }

    private inner class IsMakeBeforeRunAction : SmallBorderCheckboxAction(KotlinBundle.message("scratch.make.before.run.checkbox")) {
        override fun update(e: AnActionEvent) {
            super.update(e)
            e.presentation.isVisible = scratchFile.module != null
            e.presentation.description = scratchFile.module?.let { selectedModule ->
                KotlinBundle.message("scratch.make.before.run.checkbox.description", selectedModule.name)
            }
        }

        override fun isSelected(e: AnActionEvent): Boolean {
            return scratchFile.options.isMakeBeforeRun
        }

        override fun setSelected(e: AnActionEvent, isMakeBeforeRun: Boolean) {
            scratchFile.saveOptions { copy(isMakeBeforeRun = isMakeBeforeRun) }
        }
    }

    private inner class IsInteractiveCheckboxAction : SmallBorderCheckboxAction(
        text = KotlinBundle.message("scratch.is.interactive.checkbox"),
        description = KotlinBundle.message("scratch.is.interactive.checkbox.description", ScratchFileAutoRunner.AUTO_RUN_DELAY_IN_SECONDS)
    ) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return scratchFile.options.isInteractiveMode
        }

        override fun setSelected(e: AnActionEvent, isInteractiveMode: Boolean) {
            scratchFile.saveOptions { copy(isInteractiveMode = isInteractiveMode) }
        }
    }

    private inner class IsReplCheckboxAction : SmallBorderCheckboxAction(
        text = KotlinBundle.message("scratch.is.repl.checkbox"),
        description = KotlinBundle.message("scratch.is.repl.checkbox.description")
    ) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return scratchFile.options.isRepl
        }

        override fun setSelected(e: AnActionEvent, isRepl: Boolean) {
            scratchFile.saveOptions { copy(isRepl = isRepl) }

            if (isRepl) {
                // TODO start REPL process when checkbox is selected to speed up execution
                // Now it is switched off due to KT-18355: REPL process is keep alive if no command is executed
                //scratchFile.replScratchExecutor?.start()
            } else {
                scratchFile.replScratchExecutor?.stop()
            }
        }
    }
}
