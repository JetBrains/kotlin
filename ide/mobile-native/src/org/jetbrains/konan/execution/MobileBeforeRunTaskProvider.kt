/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.FutureResult
import org.jetbrains.konan.MobileBundle
import org.jetbrains.konan.execution.testing.MobileTestRunConfiguration
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import javax.swing.Icon

class MobileBeforeRunTaskProvider : BeforeRunTaskProvider<MobileBeforeRunTaskProvider.Task>() {
    override fun getName(): String = MobileBundle.message("build")
    override fun getId(): Key<Task> = ID
    override fun getIcon(): Icon = AllIcons.Actions.Compile
    override fun isSingleton(): Boolean = true

    override fun canExecuteTask(configuration: RunConfiguration, task: Task): Boolean =
        configuration is MobileRunConfiguration

    override fun createTask(configuration: RunConfiguration): Task? =
        if (configuration !is MobileRunConfiguration) null
        else Task()

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: Task
    ): Boolean {
        if (configuration !is MobileRunConfiguration) return false
        val device = environment.executionTarget as? Device ?: return false

        val project = configuration.project
        val projectData = ProjectDataManager.getInstance().getExternalProjectData(project, GRADLE_SYSTEM_ID, project.basePath!!)
        if (projectData == null) {
            log.warn("External project is not configured")
            return false
        }
        val moduleId = GradleProjectResolverUtil.getGradlePath(configuration.module)!!

        val settings = ExternalSystemTaskExecutionSettings()
        settings.externalSystemIdString = GRADLE_SYSTEM_ID.id
        settings.externalProjectPath = projectData.externalProjectPath
        settings.executionName = name
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
                    is AppleDevice -> listOf("$moduleId:buildIosAppMain") // TODO
                    else -> throw IllegalStateException()
                }
            else -> throw IllegalStateException()
        }

        val success = FutureResult<Boolean>()
        val callback = object : TaskCallback {
            override fun onSuccess() {
                success.set(true)
            }

            override fun onFailure() {
                success.set(false)
            }
        }

        ExternalSystemUtil.runTask(
            settings, DefaultRunExecutor.EXECUTOR_ID, project, GRADLE_SYSTEM_ID,
            callback, ProgressExecutionMode.IN_BACKGROUND_ASYNC
        )

        return success.get()
    }

    class Task : BeforeRunTask<Task>(ID) {
        init {
            isEnabled = true
        }
    }

    companion object {
        private val ID = Key<Task>("MobileBuild")
        private val log = logger<MobileBeforeRunTaskProvider>()
    }
}