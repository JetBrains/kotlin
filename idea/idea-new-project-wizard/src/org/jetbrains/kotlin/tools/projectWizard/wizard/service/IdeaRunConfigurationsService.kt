/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.execution.RunManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.tools.projectWizard.WizardGradleRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.service.RunConfigurationsService
import org.jetbrains.kotlin.tools.projectWizard.core.service.isBuildSystemAvailable
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType

class IdeaRunConfigurationsService(private val project: Project) : RunConfigurationsService, IdeaWizardService {
    override fun ValuesReadingContext.addRunConfigurations(configurations: List<WizardRunConfiguration>) {
        configurations.forEach { wizardConfiguration ->
            if (wizardConfiguration is WizardGradleRunConfiguration && isBuildSystemAvailable(BuildSystemType.GradleKotlinDsl)) {
                addGradleRunConfiguration(wizardConfiguration)
            }
        }
    }

    private fun addGradleRunConfiguration(wizardConfiguration: WizardGradleRunConfiguration) {
        val runManager = RunManager.getInstance(project)
        val configurationFactory = GradleExternalTaskConfigurationType().configurationFactories[0]
        val ideaConfiguration = runManager.createConfiguration(wizardConfiguration.configurationName, configurationFactory)
        val runConfiguration = ideaConfiguration.configuration
        if (runConfiguration is ExternalSystemRunConfiguration) {
            runConfiguration.settings.apply {
                taskNames = listOf(wizardConfiguration.taskName)
                scriptParameters = wizardConfiguration.parameters.joinToString(separator = " ")
                externalProjectPath = project.basePath
            }
            runManager.apply {
                addConfiguration(ideaConfiguration)
                selectedConfiguration = ideaConfiguration
            }
        }
    }
}