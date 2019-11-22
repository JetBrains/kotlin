/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.konan.filterOutSystemEnvs
import org.jetbrains.konan.gradle.KonanProjectResolver.Companion.KONAN_MODEL_KEY
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfigurationType
import org.jetbrains.konan.gradle.execution.GradleKonanTargetRunConfigurationProducer
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * @author Vladislav.Soroka
 */
class CLionKonanProjectDataService : AbstractProjectDataService<KonanModel, Module>() {

    override fun getTargetDataKey(): Key<KonanModel> = KONAN_MODEL_KEY

    override fun onSuccessImport(
        imported: Collection<DataNode<KonanModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        if (projectData?.owner != GradleConstants.SYSTEM_ID) return
        createRunConfigurations(project)
    }

    private fun createRunConfigurations(project: Project) {
        val workspace = GradleKonanWorkspace.getInstance(project)
        if (!workspace.isInitialized) return

        val runManager = RunManager.getInstance(project)
        var runConfigurationToSelect: RunnerAndConfigurationSettings? = null

        val configurationProducer = GradleKonanTargetRunConfigurationProducer.getGradleKonanInstance(project) ?: return
        val gradleAppRunConfigurationType = GradleKonanAppRunConfigurationType.instance

        workspace.buildTargets.map {
            // avoid adding run configurations for test executables unless there is no matching non-test (base) target
            // this is necessary to avoid polluting run configurations drop-down with too many choices
            it.baseBuildTarget ?: it
        }.forEach { buildTarget ->
            val templateConfiguration =
                    gradleAppRunConfigurationType.factory.createTemplateConfiguration(project) as GradleKonanAppRunConfiguration
            configurationProducer.setupTarget(templateConfiguration, listOf(buildTarget))

            val suggestedName = templateConfiguration.suggestedName() ?: return@forEach

            if (runManager.findConfigurationByTypeAndName(gradleAppRunConfigurationType, suggestedName) != null) {
                return@forEach
            }

            val runConfiguration = runManager.createConfiguration(suggestedName, gradleAppRunConfigurationType.factory)
            val configuration = runConfiguration.configuration as GradleKonanAppRunConfiguration
            configuration.name = suggestedName
            configurationProducer.setupTarget(configuration, listOf(buildTarget))

            /*
             * Each build configuration in a build target may have different execution parameters (working dir,
             * program parameters and environment variables). This likely is a rare case, though this is possible.
             *
             * CIDR run configuration is created as an aggregation of several build configurations, and there is
             * no way to preserve execution parameters for each of build configurations in a single created run configuration.
             * Thus, lets take exec parameters from the default build configuration (i.e. with "Debug" profile).
             *
             * TODO: In the future, we need to use the appropriate Gradle Exec tasks to run Konan binaries instead of
             * CIDR platform runner. Then we will not have any issues with passing the right exec parameters.
             */
            buildTarget.buildConfigurations
                    .filter { it.profileName == "Debug" || it.profileName == "Release" }
                    .minBy { /* Debug should go the first */ it.profileName }
                    ?.runConfiguration
                    ?.let {
                        configuration.workingDirectory = it.workingDirectory
                        configuration.programParameters = ParametersListUtil.join(it.programParameters)
                        configuration.envs = filterOutSystemEnvs(it.environmentVariables)
                    }

            runManager.addConfiguration(runConfiguration)
            if (runConfigurationToSelect == null) {
                runConfigurationToSelect = runConfiguration
            }
        }

        if (runConfigurationToSelect != null && runManager.selectedConfiguration == null) {
            val finalRunConfigurationToSelect = runConfigurationToSelect
            ApplicationManager.getApplication().invokeLater { runManager.selectedConfiguration = finalRunConfigurationToSelect }
        }
    }
}
