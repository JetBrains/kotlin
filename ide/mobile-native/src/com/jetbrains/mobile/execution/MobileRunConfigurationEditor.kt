/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.application.options.ModulesComboBox
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.jetbrains.cidr.execution.CidrRunConfigurationExecutableEditor
import com.jetbrains.cidr.execution.CidrRunConfigurationSettingsEditor
import com.jetbrains.cidr.ui.SelectExecutableActionComboItem
import com.jetbrains.mobile.MobileBundle
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import javax.swing.JPanel

open class MobileRunConfigurationEditor(
    project: Project, helper: MobileBuildConfigurationHelper,
    private val modulePredicate: (Module) -> Boolean
) : CidrRunConfigurationSettingsEditor<
        MobileBuildConfiguration,
        MobileBuildTarget,
        MobileRunConfiguration,
        MobileBuildConfigurationHelper>(project, helper) {

    private lateinit var modulesComboBox: ModulesComboBox

    private lateinit var executableEditor: CidrRunConfigurationExecutableEditor<
            MobileBuildConfiguration,
            MobileBuildTarget,
            MobileRunConfiguration,
            MobileBuildConfigurationHelper>

    override fun createEditorInner(panel: JPanel, g: GridBag) {
        val modulesLabel = JBLabel(MobileBundle.message("run.configuration.editor.module"))
        panel.add(modulesLabel, g.nextLine().next())
        modulesComboBox = ModulesComboBox()
        modulesComboBox.setModules(myProject.allModules().filter(modulePredicate))
        panel.add(modulesComboBox, g.next().coverLine())
        modulesLabel.labelFor = modulesComboBox

        super.createEditorInner(panel, g)
    }

    override fun createAdditionalControls(panel: JPanel, g: GridBag) {
        super.createAdditionalControls(panel, g)

        executableEditor = CidrRunConfigurationExecutableEditor(
            myProject,
            myConfigHelper,
            SelectExecutableActionComboItem(myProject, executableDescriptor = FileChooserDescriptorFactory
                .createSingleFileDescriptor()
                .withFileFilter { it.extension == "apk" || it.extension == "app" }),
            myConfigHelper.hasTargetsInSeveralProjects()
        )
        executableEditor.createAdditionalControls(panel, g)
    }

    override fun onTargetSelected(target: MobileBuildTarget?) {
        super.onTargetSelected(target)
        executableEditor.onTargetSelected(target)
    }

    override fun applyEditorTo(runConfiguration: MobileRunConfiguration) {
        super.applyEditorTo(runConfiguration)
        executableEditor.applyEditorTo(runConfiguration)
        runConfiguration.module = modulesComboBox.selectedModule!!
    }

    override fun resetEditorFrom(runConfiguration: MobileRunConfiguration) {
        super.resetEditorFrom(runConfiguration)
        executableEditor.resetEditorFrom(runConfiguration)
        modulesComboBox.selectedModule = runConfiguration.module
    }
}