package com.jetbrains.mobile.execution.testing

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.mobile.execution.MobileBuildConfigurationHelper
import com.jetbrains.mobile.execution.MobileRunConfigurationBase
import com.jetbrains.mobile.execution.MobileRunConfigurationEditor

class MobileTestRunConfigurationEditor(project: Project, helper: MobileBuildConfigurationHelper, modulePredicate: (Module) -> Boolean) :
    MobileRunConfigurationEditor(project, helper, modulePredicate) {

    override fun applyEditorTo(runConfiguration: MobileRunConfigurationBase) {
        val isModuleChanged = runConfiguration.module != modulesComboBox.selectedModule

        super.applyEditorTo(runConfiguration)

        if (isModuleChanged) {
            (runConfiguration as MobileTestRunConfiguration).recreateTestData()
        }
    }
}