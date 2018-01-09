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
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.uiDesigner.core.Spacer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.ScratchFileLanguageProvider
import org.jetbrains.kotlin.idea.scratch.actions.ClearScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.getScratchPanel
import javax.swing.*

val ScratchFile.scratchTopPanel: ScratchTopPanel?
    get() = getScratchPanel(psiFile)?.takeIf { it.scratchFile == this@scratchTopPanel }

class ScratchTopPanel private constructor(val scratchFile: ScratchFile) : JPanel(HorizontalLayout(5)) {
    companion object {
        fun createPanel(project: Project, virtualFile: VirtualFile): ScratchTopPanel? {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
            val scratchFile = ScratchFileLanguageProvider.createFile(psiFile) ?: return null
            return ScratchTopPanel(scratchFile)
        }
    }

    private val moduleChooser: ModulesComboBox
    private val isReplCheckbox: JCheckBox

    init {
        add(createActionsToolbar())
        add(JSeparator())
        isReplCheckbox = JCheckBox("Use Repl", false)
        add(isReplCheckbox)
        add(JSeparator())
        add(JLabel("Use classpath of module:  "))
        moduleChooser = createModuleChooser(scratchFile.psiFile.project)
        add(moduleChooser)
        add(Spacer())
    }

    fun getModule(): Module? = moduleChooser.selectedModule

    fun addModuleListener(f: (Module) -> Unit) {
        moduleChooser.addActionListener {
            moduleChooser.selectedModule?.let { f(it) }
        }
    }

    fun isRepl() = isReplCheckbox.isSelected

    @TestOnly
    fun setReplMode(isSelected: Boolean) {
        isReplCheckbox.isSelected = isSelected
    }

    private fun createActionsToolbar(): JComponent {
        val toolbarGroup = DefaultActionGroup().apply {
            add(RunScratchAction())
            addSeparator()
            add(ClearScratchAction())
        }

        return ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true).component
    }

    private fun createModuleChooser(project: Project): ModulesComboBox {
        return ModulesComboBox().apply {
            fillModules(project)
            selectedIndex = 0
        }
    }
}
