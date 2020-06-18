/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.application.options.ModulesComboBox
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.jetbrains.cidr.execution.CidrRunConfigurationSettingsEditor
import com.jetbrains.mobile.MobileBundle
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import javax.swing.JPanel

open class MobileRunConfigurationEditor(
    project: Project, helper: MobileBuildConfigurationHelper,
    private val modulePredicate: (Module) -> Boolean
) : CidrRunConfigurationSettingsEditor<
        MobileBuildConfiguration,
        MobileBuildTarget,
        MobileRunConfigurationBase,
        MobileBuildConfigurationHelper>(project, helper) {

    protected lateinit var modulesComboBox: ModulesComboBox

    override fun createEditorInner(panel: JPanel, g: GridBag) {
        val modulesLabel = JBLabel(MobileBundle.message("run.configuration.editor.module"))
        panel.add(modulesLabel, g.nextLine().next())
        modulesComboBox = ModulesComboBox()
        modulesComboBox.setModules(myProject.allModules().filter(modulePredicate))
        panel.add(modulesComboBox, g.next().coverLine())
        modulesLabel.labelFor = modulesComboBox

        super.createEditorInner(panel, g)
    }

    override fun applyEditorTo(runConfiguration: MobileRunConfigurationBase) {
        super.applyEditorTo(runConfiguration)
        runConfiguration.module = modulesComboBox.selectedModule
    }

    override fun resetEditorFrom(runConfiguration: MobileRunConfigurationBase) {
        super.resetEditorFrom(runConfiguration)
        modulesComboBox.selectedModule = runConfiguration.module
    }
}