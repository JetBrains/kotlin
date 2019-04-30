/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NonNls

/**
 * @author Vladislav.Soroka
 */
class GradleKonanAppRunConfigurationType private constructor() : ConfigurationTypeBase(
        "GradleKonanAppRunConfiguration",
        "Kotlin/Native Application",
        "Kotlin/Native application configuration",
        AllIcons.RunConfigurations.Application
) {

    private val myDefaultFactoryId: String = "Application"

    val factory: ConfigurationFactory
        get() = object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return createRunConfiguration(project, this)
            }

            @NonNls
            override fun getId(): String {
                return myDefaultFactoryId
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
