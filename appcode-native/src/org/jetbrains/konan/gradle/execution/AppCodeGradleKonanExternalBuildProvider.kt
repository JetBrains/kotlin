/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildConfiguration
import com.jetbrains.cidr.execution.build.XcodeBuildAction
import com.jetbrains.cidr.execution.build.XcodeExternalBuildProvider
import com.jetbrains.cidr.xcode.frameworks.buildSystem.BuildSettingNames
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.plugins.gradle.settings.GradleSettings


class AppCodeGradleKonanExternalBuildProvider : XcodeExternalBuildProvider {
    companion object {
        private const val KOTLIN_NATIVE_BUILD_CAPABLE = "KOTLIN_NATIVE_BUILD_CAPABLE"
        private const val SDK_NAME = "SDK_NAME"

        private const val GRADLE_CLEAN_TASK_NAME = "clean"
        private const val GRADLE_BUILD_TASK_NAME = "buildForXcode"
    }

    private data class GradleBuildContext(
        val name: String,
        val configurationName: String,
        val sdkName: String,
        val configurationBuildDir: String,
        val frameworkNames: List<String>
    )

    override fun getAdditionalBuildProperties(): Map<String, String> = mapOf(
        KOTLIN_NATIVE_BUILD_CAPABLE to "YES"
    )

    private fun getTasksToInvoke(taskName: String, configuration: BuildConfiguration): List<String> {
        val configurations = mutableListOf(configuration)
        configurations.addAll(configuration.target.resolvedDependencies
                                  .filter { it.isFramework }
                                  .mapNotNull { configuration.getWithTarget(it) })

        val tasks = HashSet<String>()
        KonanProjectDataService.forEachKonanProject(configuration.project) { _, moduleNode, _ ->
            tasks.addAll(ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.TASK).map { it.data.name })
        }

        return configurations
            .mapNotNull { it.getBuildSetting(BuildSettingNames.TARGET_NAME).string }
            .map { ":$it:$taskName" }
            .filter { it in tasks }
    }

    override fun beforeBuild(project: Project, configuration: BuildConfiguration, action: XcodeBuildAction) {
        if (GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
            return
        }

        val (taskName, taskDescription) = when (action) {
            XcodeBuildAction.BUILD -> GRADLE_BUILD_TASK_NAME to "Build"
            XcodeBuildAction.CLEAN -> GRADLE_CLEAN_TASK_NAME to "Clean"
        }

        var rootPath: String? = null
        KonanProjectDataService.forEachKonanProject(project) { _, _, rootProjectPath ->
            rootPath = rootProjectPath
        }

        val frameworksToBuild = runReadAction {
            GradleBuildContext(
                configuration.configurationName,
                configuration.getBuildSetting(BuildSettingNames.CONFIGURATION).string!!,
                configuration.sdk.name,
                configuration.getBuildSetting(BuildSettingNames.CONFIGURATION_BUILD_DIR).string!!,
                getTasksToInvoke(taskName, configuration)
            )
        }

        frameworksToBuild.let {
            it.frameworkNames.forEach { gradleTaskName ->
                GradleKonanBuild.runBuildTasks(
                    project,
                    "$taskDescription ${it.name}",
                    listOf(gradleTaskName),
                    rootPath!!,
                    true,
                    mapOf(
                        SDK_NAME to it.sdkName,
                        BuildSettingNames.CONFIGURATION to it.configurationName,
                        BuildSettingNames.CONFIGURATION_BUILD_DIR to it.configurationBuildDir
                    )
                )
            }
        }
    }
}