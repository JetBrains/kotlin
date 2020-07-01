package org.jetbrains.konan.gradle.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.konan.*
import org.jetbrains.annotations.NonNls

/**
 * @author Vladislav.Soroka
 */
class GradleKonanAppRunConfigurationType private constructor() : ConfigurationTypeBase(
    KonanBundle.message("id.runConfiguration"),
    KonanBundle.message("label.applicationName.text"),
    KonanBundle.message("label.applicationDescription.text"),
    AllIcons.RunConfigurations.Application
) {
    val factory: ConfigurationFactory
        get() = object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return createRunConfiguration(project, this)
            }

            @NonNls
            override fun getId(): String {
                return KonanBundle.message("id.factory")
            }
        }

    init {
        addFactory(factory)
    }

    private fun createRunConfiguration(project: Project, factory: ConfigurationFactory): GradleKonanAppRunConfiguration {
        return GradleKonanAppRunConfiguration(project, factory, "")
    }

    fun createEditor(project: Project): SettingsEditor<out GradleKonanAppRunConfiguration> {
        return GradleKonanAppRunConfigurationSettingsEditor(project, getHelper(project))
    }

    companion object {
        fun getHelper(project: Project): GradleKonanBuildConfigurationHelper {
            return GradleKonanBuildConfigurationHelper(project)
        }

        val instance: GradleKonanAppRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(GradleKonanAppRunConfigurationType::class.java)
    }
}
