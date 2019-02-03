/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildConfiguration
import com.jetbrains.cidr.execution.build.XcodeBuildAction
import com.jetbrains.cidr.execution.build.XcodeExternalBuildProvider
import com.jetbrains.cidr.xcode.frameworks.buildSystem.BuildSettingNames
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.plugins.gradle.settings.GradleSettings

class AppCodeGradleKonanExternalBuildProvider : XcodeExternalBuildProvider {

    companion object {
        private const val KOTLIN_NATIVE_PRESET = "KOTLIN_NATIVE_PRESET"
        private const val KOTLIN_NATIVE_BUILD_CAPABLE = "KOTLIN_NATIVE_BUILD_CAPABLE"

        private const val GRADLE_CLEAN_TASK_NAME = "clean"
        private const val GRADLE_BUILD_TASK_NAME = "buildFramework"

        private const val PRESET_NAME = "preset.name"
        private const val CONFIGURATION_NAME = "configuration.name"
        private const val CONFIGURATION_BUILD_DIR = "configuration.build.dir"
    }

    private data class GradleFrameworkDescriptor(
        val name: String,
        val presetName: String
    )

    private data class GradleBuildContext(
        val name: String,
        val configurationName: String,
        val configurationBuildDir: String,
        val frameworks: List<GradleFrameworkDescriptor>
    )

    override fun getAdditionalBuildProperties(): Map<String, String> = mapOf(
        KOTLIN_NATIVE_BUILD_CAPABLE to "YES"
    )

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
                configuration.getBuildSetting(BuildSettingNames.CONFIGURATION_BUILD_DIR).string!!,
                configuration.target.resolvedDependencies
                    .filter { it.isFramework }
                    .mapNotNull { configuration.getWithTarget(it) }
                    .mapNotNull {
                        val preset = it.getBuildSetting(KOTLIN_NATIVE_PRESET).string ?: return@mapNotNull null
                        val name = it.getBuildSetting(BuildSettingNames.PRODUCT_NAME).string!!

                        GradleFrameworkDescriptor(name, preset)
                    }
            )
        }

        frameworksToBuild.let {
            it.frameworks.forEach { framework ->
                runBuildTasks(
                    project,
                    "$taskDescription ${it.name}",
                    listOf("${framework.name}:$taskName"),
                    rootPath!!,
                    true,
                    listOf(
                        PRESET_NAME to framework.presetName,
                        CONFIGURATION_NAME to it.configurationName,
                        CONFIGURATION_BUILD_DIR to it.configurationBuildDir
                    ).joinToString(" ") { (name, value) -> "-P$name=\"$value\"" }
                )
            }
        }

    }

}