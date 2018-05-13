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
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.panels.HorizontalLayout
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.ScratchFileLanguageProvider
import org.jetbrains.kotlin.idea.scratch.actions.ClearScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.addScratchPanel
import org.jetbrains.kotlin.idea.scratch.removeScratchPanel
import javax.swing.*

class ScratchTopPanel private constructor(val scratchFile: ScratchFile) : JPanel(HorizontalLayout(5)), Disposable {
    override fun dispose() {
        scratchFile.editor.removeScratchPanel(this)
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

    init {
        add(createActionsToolbar())

        moduleChooser = createModuleChooser(scratchFile.project)
        add(JLabel("Use classpath of module"))
        add(moduleChooser)

        add(JSeparator(SwingConstants.VERTICAL))

        isReplCheckbox = JCheckBox("Use REPL", false)
        add(isReplCheckbox)
        isReplCheckbox.addItemListener {
            scratchFile.getPsiFile()?.virtualFile?.apply {
                scratchPanelConfig = (scratchPanelConfig ?: ScratchPanelConfig()).copy(isRepl = isReplCheckbox.isSelected)
            }
        }

        add(JSeparator(SwingConstants.VERTICAL))

        isMakeBeforeRunCheckbox = JCheckBox("Make before Run", false)
        add(isMakeBeforeRunCheckbox)
        isMakeBeforeRunCheckbox.addItemListener {
            scratchFile.getPsiFile()?.virtualFile?.apply {
                scratchPanelConfig = (scratchPanelConfig ?: ScratchPanelConfig()).copy(isMakeBeforeRun = isMakeBeforeRunCheckbox.isSelected)
            }
        }

        add(JSeparator(SwingConstants.VERTICAL))

        (scratchFile.getPsiFile()?.virtualFile?.scratchPanelConfig ?: ScratchPanelConfig()).let {
            isReplCheckbox.isSelected = it.isRepl
            isMakeBeforeRunCheckbox.isSelected = it.isMakeBeforeRun
        }
    }

    fun getModule(): Module? = moduleChooser.selectedModule

    fun setModule(module: Module) {
        moduleChooser.selectedModule = module
    }

    fun addModuleListener(f: (PsiFile, Module) -> Unit) {
        moduleChooser.addActionListener {
            val selectedModule = moduleChooser.selectedModule
            val psiFile = scratchFile.getPsiFile()
            if (selectedModule != null && psiFile != null) {
                f(psiFile, selectedModule)
            }
        }
    }

    fun isRepl() = isReplCheckbox.isSelected
    fun isMakeBeforeRun() = isMakeBeforeRunCheckbox.isSelected

    @TestOnly
    fun setReplMode(isSelected: Boolean) {
        isReplCheckbox.isSelected = isSelected
    }

    @TestOnly
    fun setMakeBeforeRun(isSelected: Boolean) {
        isMakeBeforeRunCheckbox.isSelected = isSelected
    }

    private fun createActionsToolbar(): JComponent {
        val toolbarGroup = DefaultActionGroup().apply {
            add(RunScratchAction(this@ScratchTopPanel))
            addSeparator()
            add(ClearScratchAction(this@ScratchTopPanel))
        }

        return ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true).component
    }

    private fun createModuleChooser(project: Project): ModulesComboBox {
        return ModulesComboBox().apply {
            fillModules(project)
        }
    }
}

data class ScratchPanelConfig(val isRepl: Boolean = false, val isMakeBeforeRun: Boolean = false)
