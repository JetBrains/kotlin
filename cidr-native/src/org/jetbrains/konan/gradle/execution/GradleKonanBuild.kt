/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.build.BuildViewManager
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectModelBuildTask
import com.intellij.util.concurrency.Semaphore
import com.jetbrains.cidr.execution.BuildConfigurationProblems
import org.jetbrains.konan.KonanBundle.message
import org.jetbrains.konan.gradle.GradleKonanWorkspace
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration.BuildAndRunConfigurations
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * TODO: Drop this class when #IDEA-204372 and #KT-28880 are fixed and the appropriate instances of
 * [ModuleBuildTask] and [ProjectModelBuildTask] for building all flavors of Kotlin modules (including Kotlin/Native)
 * are implemented.
 *
 * @author Vladislav.Soroka
 */
object GradleKonanBuild {

    fun buildProject(project: Project, cleanupNeeded: Boolean, compileNeeded: Boolean) {
        if (!cleanupNeeded && !compileNeeded) return

        val buildModules = GradleKonanWorkspace.getInstance(project).buildModules
        if (buildModules.isEmpty()) return

        val projectPaths = mutableSetOf<String>()
        val cleanupTasks = mutableSetOf<String>()
        val compileTasks = mutableSetOf<String>()

        buildModules.forEach { buildModule ->
            projectPaths += buildModule.projectPath
            if (cleanupNeeded) cleanupTasks.addIfNotNull(buildModule.moduleCleanTaskName)
            if (compileNeeded) compileTasks.addIfNotNull(buildModule.moduleBuildTaskName)
        }

        val executionName = when (compileTasks.isEmpty()) {
            true -> when (cleanupTasks.isEmpty()) {
                true -> return
                else -> message("execution.cleanProject.name", project.name)
            }
            false -> when (cleanupTasks.isEmpty()) {
                true -> message("execution.buildProject.name", project.name)
                false -> message("execution.rebuildProject.name", project.name)
            }
        }

        if (projectPaths.size > 1) {
            failBuild(project, executionName, message("error.multiple.external.project.paths"))
            return
        }

        val projectPath = projectPaths.first()
        val tasks = (cleanupTasks.asSequence() + compileTasks.asSequence()).toList()

        runBuildTasks(project, executionName, tasks, projectPath, true)
    }

    fun buildConfiguration(project: Project, cleanupNeeded: Boolean, configuration: GradleKonanAppRunConfiguration) {
        val executionName = if (cleanupNeeded)
            message("execution.rebuildConfiguration.name", configuration.name)
        else
            message("execution.buildConfiguration.name", configuration.name)

        val buildConfiguration = getBuildAndRunConfigurations(
            configuration,
            ExecutionTargetManager.getActiveTarget(project),
            executionName
        )?.buildConfiguration ?: return

        val tasks = mutableListOf<String>()
        if (cleanupNeeded) tasks.addIfNotNull(buildConfiguration.artifactCleanTaskName)
        tasks += buildConfiguration.artifactBuildTaskName

        runBuildTasks(project, executionName, tasks, buildConfiguration.projectPath, true)
    }

    fun compileBeforeRun(project: Project, environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): Boolean {
        val buildConfiguration = getBuildAndRunConfigurations(
            configuration,
            environment.executionTarget,
            environment.runProfile.name
        )?.buildConfiguration ?: return false

        return with(buildConfiguration) {
            runBuildTasks(project, message("execution.compileConfiguration.name", name), listOf(artifactBuildTaskName), projectPath, false)
        }
    }
}

private fun getBuildAndRunConfigurations(
    configuration: GradleKonanAppRunConfiguration,
    target: ExecutionTarget,
    executionName: String
): BuildAndRunConfigurations? {
    val problems = BuildConfigurationProblems()
    val buildAndRunConfigurations = configuration.getBuildAndRunConfigurations(target, problems, false)
    if (buildAndRunConfigurations == null) {
        if (problems.hasProblems()) {
            handleExecutionError(configuration.project, executionName, problems)
        }
        return null
    }
    return buildAndRunConfigurations
}

private fun runBuildTasks(
    project: Project,
    executionName: String,
    taskNames: List<String>,
    projectPath: String,
    activateToolWindowBeforeRun: Boolean
): Boolean {
    val settings = ExternalSystemTaskExecutionSettings().apply {
        this.executionName = executionName
        externalProjectPath = projectPath
        this.taskNames = taskNames
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
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

private fun handleExecutionError(project: Project, executionName: String, problems: BuildConfigurationProblems) {
    if (problems.hasProblems()) {
        ExecutionUtil.handleExecutionError(project, ToolWindowId.BUILD, executionName, ExecutionException(problems.htmlProblems))
    }
}

private fun failBuild(project: Project, executionName: String, errorMessage: String) {
    ApplicationManager.getApplication().invokeLater {
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.BUILD)?.activate { }
        handleExecutionError(project, executionName, BuildConfigurationProblems().apply { problems += errorMessage })
    }
}
