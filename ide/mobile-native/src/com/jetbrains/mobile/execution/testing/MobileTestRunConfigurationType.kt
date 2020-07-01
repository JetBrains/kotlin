package com.jetbrains.mobile.execution.testing

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.jetbrains.mobile.MobileBundle

class MobileTestRunConfigurationType : ConfigurationTypeBase(
    ID,
    MobileBundle.message("run.configuration.test.name"),
    MobileBundle.message("run.configuration.test.description"),
    AllIcons.RunConfigurations.Junit
) {
    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId(): String = ID

            override fun createTemplateConfiguration(project: Project): RunConfiguration =
                MobileTestRunConfiguration(project, this, name)
        })
    }

    companion object {
        private const val ID = "KonanMobileTest"
    }
}