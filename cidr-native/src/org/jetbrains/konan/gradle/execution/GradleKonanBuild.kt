/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.build.BuildViewManager
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil.handleExecutionError
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectModelBuildTask
import com.intellij.util.concurrency.Semaphore
import com.jetbrains.cidr.execution.BuildConfigurationProblems
import org.jetbrains.konan.KonanBundle.message
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration.BuildAndRunConfigurations
import org.jetbrains.plugins.gradle.util.GradleConstants

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

    fun runBuildTasks(
        project: Project,
        executionName: String,
        taskNames: List<String>,
        projectPath: String,
        activateToolWindowBeforeRun: Boolean,
        env: Map<String, String>? = null
    ): Boolean {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            this.executionName = executionName
            externalProjectPath = projectPath
            this.taskNames = taskNames
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            env?.let { this.env = it }
        }

        val userData = UserDataHolderBase()
        userData.putUserData(PROGRESS_LISTENER_KEY, BuildViewManager::class.java)

        val result = Ref.create(false)
        val finished = Semaphore(1)
        val taskCallback = object : TaskCallback {
            override fun onSuccess() {
                result.set(true)
                finished.up()
            }

            override fun onFailure() {
                result.set(false)
                finished.up()
            }
        }

        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            taskCallback,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            activateToolWindowBeforeRun,
            userData
        )
        finished.waitFor()
        return result.get()
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
