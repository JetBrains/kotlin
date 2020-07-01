package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildConfiguration
import com.jetbrains.cidr.execution.build.XcodeBuildAction
import com.jetbrains.cidr.execution.build.XcodeExternalBuildProvider
import com.jetbrains.cidr.xcode.frameworks.buildSystem.BuildSettingNames
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.konan.runBuildTasks
import org.jetbrains.konan.gradle.forEachKonanProject
import org.jetbrains.plugins.gradle.settings.GradleSettings


class AppCodeGradleKonanExternalBuildProvider : XcodeExternalBuildProvider {
    companion object {
        private const val KOTLIN_NATIVE_BUILD_CAPABLE = "KOTLIN_NATIVE_BUILD_CAPABLE"
        private const val SDK_NAME = "SDK_NAME"

        private const val GRADLE_CLEAN_TASK_NAME = "clean"
        const val GRADLE_BUILD_TASK_NAME = "buildForXcode"
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
        val target = configuration.target
        configurations.addAll(target.resolvedDependencies.asSequence()
                                  .filter { it.isFramework }
                                  .mapNotNull { configuration.getWithTarget(it) })

        return configurations.asSequence()
            .mapNotNull { it.getBuildSetting(BuildSettingNames.TARGET_NAME).string }
            .mapGradleTasks(taskName, configuration.project).toList()
    }

    override fun beforeBuild(project: Project, configuration: BuildConfiguration, action: XcodeBuildAction) {
        if (GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
            return
        }

        val (taskName, taskDescription) = when (action) {
            XcodeBuildAction.BUILD_FOR_LAUNCH, XcodeBuildAction.BUILD_FOR_TEST -> GRADLE_BUILD_TASK_NAME to "Build"
            XcodeBuildAction.CLEAN_FOR_LAUNCH, XcodeBuildAction.CLEAN_FOR_TEST -> GRADLE_CLEAN_TASK_NAME to "Clean"
            else -> TODO()
        }

        val rootPath = getKonanRootProjectPath(project)

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
                runBuildTasks(
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

fun Sequence<PBXTarget>.filterGradleTasks(taskName: String, project: Project): Sequence<PBXTarget> {
    val tasks = collectsGradleTasks(project)
    return filter { target -> target.isFramework && ":${target.name}:$taskName" in tasks }
}

fun Sequence<String>.mapGradleTasks(taskName: String, project: Project): Sequence<String> {
    val tasks = collectsGradleTasks(project)
    return map { targetName -> ":$targetName:$taskName" }.filter { task -> task in tasks }
}

private fun collectsGradleTasks(project: Project): Set<String> {
    val tasks = HashSet<String>()
    forEachKonanProject(project) { _, moduleNode, _ ->
        for (taskNode in ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.TASK)) {
            tasks.add(taskNode.data.name)
        }
    }
    return tasks
}

private fun getKonanRootProjectPath(project: Project): String? {
    forEachKonanProject(project) { _, _, rootProjectPath ->
        return rootProjectPath
    }
    return null
}
