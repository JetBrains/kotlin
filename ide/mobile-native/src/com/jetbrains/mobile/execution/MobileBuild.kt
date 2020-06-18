/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.build.BuildContentManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemTaskRunner
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.concurrency.FutureResult
import com.jetbrains.mobile.MobileBundle
import com.jetbrains.mobile.execution.testing.MobileTestRunConfiguration
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil

object MobileBuild {
    fun build(configuration: MobileRunConfigurationBase, device: Device): Boolean {
        val project = configuration.project
        val projectData = ProjectDataManager.getInstance().getExternalProjectData(project, GRADLE_SYSTEM_ID, project.basePath!!)
        if (projectData == null) {
            log.warn("External project is not configured")
            return false
        }
        val moduleId = GradleProjectResolverUtil.getGradlePath(configuration.module!!)!!

        val settings = ExternalSystemTaskExecutionSettings()
        settings.externalSystemIdString = GRADLE_SYSTEM_ID.id
        settings.externalProjectPath = projectData.externalProjectPath
        settings.executionName = MobileBundle.message("build")
        settings.taskNames = when (configuration) {
            is MobileAppRunConfiguration ->
                when (device) {
                    is AndroidDevice -> listOf("$moduleId:assembleDebug")
                    is AppleDevice -> listOf("$moduleId:buildIosAppMain")
                    else -> throw IllegalStateException()
                }
            is MobileTestRunConfiguration ->
                when (device) {
                    is AndroidDevice -> listOf("$moduleId:assembleDebug", "$moduleId:assembleDebugAndroidTest")
                    is AppleDevice -> listOf("$moduleId:buildIosAppTest")
                    else -> throw IllegalStateException()
                }
            else -> throw IllegalStateException()
        }

        val success = FutureResult<Boolean>()
        val callback = object : TaskCallback {
            override fun onSuccess() {
                success.set(
                    try {
                        ProgressManager.checkCanceled()
                        true
                    } catch (e: ProcessCanceledException) {
                        false
                    }
                )
            }

            override fun onFailure() {
                success.set(false)
            }
        }

        val toolWindowId = invokeAndWaitIfNeeded {
            BuildContentManager.getInstance(project).getOrCreateToolWindow().id
        }
        ExternalSystemUtil.runTask(
            settings, DefaultRunExecutor.EXECUTOR_ID, project, GRADLE_SYSTEM_ID,
            callback, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false,
            UserDataHolderBase().also {
                it.putUserData(ExternalSystemTaskRunner.TOOL_WINDOW_ID_KEY, toolWindowId)
            }
        )

        return success.get()
    }

    private val log = logger<MobileBuild>()
}