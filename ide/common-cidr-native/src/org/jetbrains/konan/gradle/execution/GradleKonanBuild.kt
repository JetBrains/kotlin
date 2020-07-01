package org.jetbrains.konan.gradle.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil.handleExecutionError
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectModelBuildTask
import com.jetbrains.cidr.execution.BuildConfigurationProblems
import com.jetbrains.konan.KonanBundle.message
import com.jetbrains.konan.runBuildTasks
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration.BuildAndRunConfigurations

/**
 * TODO: Drop this class when #IDEA-204372 and #KT-28880 are fixed and the appropriate instances of
 * [ModuleBuildTask] and [ProjectModelBuildTask] for building all flavors of Kotlin modules (including Kotlin/Native)
 * are implemented.
 *
 * @author Vladislav.Soroka
 */
object GradleKonanBuild {

    fun buildBeforeRun(project: Project, environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): Boolean {
        val buildConfiguration = getBuildAndRunConfigurations(
            configuration,
            environment.executionTarget,
            environment.runProfile.name
        )?.buildConfiguration ?: return false

        return with(buildConfiguration) {
            runBuildTasks(project, message("execution.buildConfiguration.name", name), listOf(artifactBuildTaskPath), projectPath, false)
        }
    }
}

private fun getBuildAndRunConfigurations(
    configuration: GradleKonanAppRunConfiguration,
    target: ExecutionTarget,
    executionName: String
): BuildAndRunConfigurations? {
    val problems = BuildConfigurationProblems()
    configuration.getBuildAndRunConfigurations(target, problems, false)?.also { return it }

    if (problems.hasProblems())
        handleExecutionError(configuration.project, ToolWindowId.BUILD, executionName, ExecutionException(problems.htmlProblems))

    return null
}
