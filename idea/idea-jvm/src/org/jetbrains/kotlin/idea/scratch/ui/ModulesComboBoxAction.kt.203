/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.ui

import com.intellij.core.JavaPsiBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.changes.committed.LabeledComboBoxAction
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.isKotlinWorksheet
import java.util.function.Supplier
import javax.swing.JComponent

class ModulesComboBoxAction(private val scratchFile: ScratchFile) :
    LabeledComboBoxAction(KotlinJvmBundle.message("scratch.module.combobox"))
{
    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(ModuleIsNotSelectedAction(JavaPsiBundle.messagePointer("list.item.no.module")))

        val modules = ModuleManager.getInstance(scratchFile.project).modules.filter {
            it.productionSourceInfo() != null || it.testSourceInfo() != null
        }

        actionGroup.addAll(modules.map { SelectModuleAction(it) })

        return actionGroup
    }

    /**
     * By default this action uses big font for label, so we have to decrease it
     * to make it look the same as in [CheckboxAction].
     */
    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val customComponent = super.createCustomComponent(presentation, place)
        customComponent.components.forEach { it.font = UIUtil.getFont(UIUtil.FontSize.SMALL, it.font) }
        return customComponent
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedModule = scratchFile.module?.takeIf { !it.isDisposed }

        e.presentation.apply {
            icon = selectedModule?.let { ModuleType.get(it).icon }
            text = selectedModule?.name ?: JavaPsiBundle.message("list.item.no.module")
        }

        e.presentation.isVisible = isModuleSelectorVisible()
    }

    @TestOnly
    fun isModuleSelectorVisible(): Boolean {
        return !scratchFile.file.isKotlinWorksheet
    }

    private inner class ModuleIsNotSelectedAction(placeholder: Supplier<String>) : DumbAwareAction(placeholder) {
        override fun actionPerformed(e: AnActionEvent) {
            scratchFile.setModule(null)
        }
    }

    private inner class SelectModuleAction(private val module: Module) :
        DumbAwareAction(module.name, null, ModuleType.get(module).icon) {
        override fun actionPerformed(e: AnActionEvent) {
            scratchFile.setModule(module)
        }
    }
}