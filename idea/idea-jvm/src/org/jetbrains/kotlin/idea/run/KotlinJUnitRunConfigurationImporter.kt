/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.junit.JUnitRunConfigurationImporter
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.settings.RunConfigurationImporter
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

@Suppress("UnstableApiUsage")
class KotlinJUnitRunConfigurationImporter : RunConfigurationImporter {
    private fun Any?.isTrue(): Boolean = this != null && this is Boolean && this

    override fun canImport(typeName: String) = "junit" == typeName

    private fun getOriginalImporter(): JUnitRunConfigurationImporter {
        return RunConfigurationImporter.EP_NAME.extensions.firstIsInstance()
    }

    override fun getConfigurationFactory() = getOriginalImporter().configurationFactory

    override fun process(
        project: Project,
        runConfiguration: RunConfiguration,
        cfg: MutableMap<String, Any>,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        getOriginalImporter().process(project, runConfiguration, cfg, modelsProvider)

        if (runConfiguration !is KotlinJUnitConfiguration && cfg["defaults"].isTrue()) {
            val runManager = RunManagerEx.getInstanceEx(project)
            val configurationFactory = ConfigurationTypeUtil
                .findConfigurationType(KotlinJUnitConfigurationType::class.java).configurationFactories[0]
            val kotlinRunnerAndConfigurationSettings = runManager.getConfigurationTemplate(configurationFactory)
            process(project, kotlinRunnerAndConfigurationSettings.configuration, cfg, modelsProvider)

            (cfg["beforeRun"] as? List<*>)?.let {
                val beforeRunTasks = runManager.getBeforeRunTasks(runConfiguration)
                runManager.setBeforeRunTasks(kotlinRunnerAndConfigurationSettings.configuration, beforeRunTasks)
            }
        }
    }
}