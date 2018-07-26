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
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.ObjectUtils
import com.intellij.util.concurrency.Semaphore
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.execution.BuildConfigurationProblems
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration.BuildAndRunConfigurations
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * @author Vladislav.Soroka
 */
object GradleKonanBuild {

  fun build(project: Project, environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): Boolean {
    val target = environment.executionTarget

    val buildAndRunConfigurations = getBuildAndRunConfigurations(configuration, environment, target) ?: return false

    return build(project, buildAndRunConfigurations)
  }

  fun build(project: Project, buildAndRunConfigurations: BuildAndRunConfigurations): Boolean {
    val buildConfiguration = buildAndRunConfigurations.buildConfiguration
    val settings = ExternalSystemTaskExecutionSettings()

    val compileTaskName = buildConfiguration.compileTaskName
    val taskName = ObjectUtils.chooseNotNull(null, compileTaskName) ?: return false

    val executionName = "Compile " + buildConfiguration.name
    settings.executionName = executionName
    settings.externalProjectPath = buildConfiguration.projectPath
    settings.taskNames = ContainerUtil.list(taskName)
    settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
    settings.scriptParameters = "-Pkonan.debugging.symbols=true -Pkonan.optimizations.enable=false"

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
    ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
                               taskCallback, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false, userData)
    finished.waitFor()
    return result.get()
  }

  private fun getBuildAndRunConfigurations(configuration: GradleKonanAppRunConfiguration,
                                           environment: ExecutionEnvironment,
                                           target: ExecutionTarget): BuildAndRunConfigurations? {
    val problems = BuildConfigurationProblems()
    val buildAndRunConfigurations = configuration.getBuildAndRunConfigurations(target, problems, false)
    if (buildAndRunConfigurations == null) {
      if (problems.hasProblems()) {
        ExecutionUtil.handleExecutionError(environment, ExecutionException(problems.htmlProblems))
      }
      return null
    }
    return buildAndRunConfigurations
  }
}
