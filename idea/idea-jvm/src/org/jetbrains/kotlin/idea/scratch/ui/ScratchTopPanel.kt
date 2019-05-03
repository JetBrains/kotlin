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


import com.intellij.application.options.ModulesComboBox
import com.intellij.execution.ui.ConfigurationModuleSelector
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.ScratchFileLanguageProvider
import org.jetbrains.kotlin.idea.scratch.actions.ClearScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.StopScratchAction
import org.jetbrains.kotlin.idea.scratch.addScratchPanel
import org.jetbrains.kotlin.idea.scratch.removeScratchPanel
import javax.swing.*

class ScratchTopPanel private constructor(val scratchFile: ScratchFile) : JPanel(HorizontalLayout(5)), Disposable {
    override fun dispose() {
        scratchFile.editor.removeScratchPanel()
    }

    companion object {
        fun createPanel(project: Project, virtualFile: VirtualFile, editor: TextEditor) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
            val scratchFile = ScratchFileLanguageProvider.get(psiFile.language)?.createFile(project, editor) ?: return
            editor.addScratchPanel(ScratchTopPanel(scratchFile))
        }
    }

    private val moduleChooser: ModulesComboBox
    private val isReplCheckbox: JCheckBox
    private val isMakeBeforeRunCheckbox: JCheckBox
    private val isInteractiveCheckbox: JCheckBox

    private val moduleSeparator: JSeparator
    private val actionsToolbar: ActionToolbar

    init {
        actionsToolbar = createActionsToolbar()
        add(actionsToolbar.component)

        moduleChooser = createModuleChooser(scratchFile.project)
        add(JLabel("Use classpath of module"))
        add(moduleChooser)

        isMakeBeforeRunCheckbox = JCheckBox("Make before Run")
        add(isMakeBeforeRunCheckbox)
        isMakeBeforeRunCheckbox.addItemListener {
            scratchFile.saveOptions {
                copy(isMakeBeforeRun = isMakeBeforeRunCheckbox.isSelected)
            }
        }

        moduleSeparator = JSeparator(SwingConstants.VERTICAL)
        add(moduleSeparator)

        changeMakeModuleCheckboxVisibility(false)

        isInteractiveCheckbox = JCheckBox("Interactive mode")
        add(isInteractiveCheckbox)
        isInteractiveCheckbox.addItemListener {
            scratchFile.saveOptions {
                copy(isInteractiveMode = isInteractiveCheckbox.isSelected)
            }
        }

        add(JSeparator(SwingConstants.VERTICAL))

        isReplCheckbox = JCheckBox("Use REPL")
        add(isReplCheckbox)
        isReplCheckbox.addItemListener {
            scratchFile.saveOptions {
                copy(isRepl = isReplCheckbox.isSelected)
            }
        }

        add(JSeparator(SwingConstants.VERTICAL))

        scratchFile.options.let {
            isReplCheckbox.isSelected = it.isRepl
            isMakeBeforeRunCheckbox.isSelected = it.isMakeBeforeRun
            isInteractiveCheckbox.isSelected = it.isInteractiveMode
        }
    }

    fun getModule(): Module? = moduleChooser.selectedModule

    fun setModule(module: Module) {
        moduleChooser.selectedModule = module
    }

    fun addModuleListener(f: (PsiFile, Module?) -> Unit) {
        moduleChooser.addActionListener {
            val selectedModule = moduleChooser.selectedModule

            changeMakeModuleCheckboxVisibility(selectedModule != null)

            val psiFile = scratchFile.getPsiFile()
            if (psiFile != null) {
                f(psiFile, selectedModule)
            }
        }
    }

    @TestOnly
    fun setReplMode(isSelected: Boolean) {
        isReplCheckbox.isSelected = isSelected
    }

    @TestOnly
    fun setMakeBeforeRun(isSelected: Boolean) {
        isMakeBeforeRunCheckbox.isSelected = isSelected
    }

    @TestOnly
    fun setInteractiveMode(isSelected: Boolean) {
        isInteractiveCheckbox.isSelected = isSelected
    }

    private fun changeMakeModuleCheckboxVisibility(isVisible: Boolean) {
        isMakeBeforeRunCheckbox.isVisible = isVisible
        moduleSeparator.isVisible = isVisible
    }

    fun updateToolbar() {
        ApplicationManager.getApplication().invokeLater {
            actionsToolbar.updateActionsImmediately()
        }
    }

    private fun createActionsToolbar(): ActionToolbar {
        val toolbarGroup = DefaultActionGroup().apply {
            add(RunScratchAction())
            add(StopScratchAction())
            addSeparator()
            add(ClearScratchAction())
        }

        return ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true)
    }

    private fun createModuleChooser(project: Project): ModulesComboBox {
        return ModulesComboBox().apply {
            setModules(ModuleManager.getInstance(project).modules.filter {
                it.productionSourceInfo() != null || it.testSourceInfo() != null
            })
            allowEmptySelection(ConfigurationModuleSelector.NO_MODULE_TEXT)
        }
    }
}

interface ScratchPanelListener {
    fun panelAdded(panel: ScratchTopPanel)

    companion object {
        val TOPIC = Topic.create("ScratchPanelListener", ScratchPanelListener::class.java)
    }
}
