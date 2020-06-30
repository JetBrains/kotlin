/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios.execution

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import com.jetbrains.cidr.OCPathManagerCustomization
import com.jetbrains.mpp.debugger.LLDBBackendBase.Companion.DEBUG_SERVER_PATH_KEY
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings

class BinaryGradleTaskManager : GradleTaskManagerExtension {
    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) = true

    override fun executeTasks(
        id: ExternalSystemTaskId,
        taskNames: MutableList<String>,
        projectPath: String,
        settings: GradleExecutionSettings?,
        jvmParametersSetup: String?,
        listener: ExternalSystemTaskNotificationListener
    ): Boolean {
        val params = HashMap<String, String>()

        val debugServerPath = "LLDB.framework/Resources/debugserver"
        params[DEBUG_SERVER_PATH_KEY] = OCPathManagerCustomization.getInstance().getBinFile(debugServerPath).absolutePath

        val serializedParams = params.map { (key, value) -> "$key=$value" }.joinToString(separator = ForkedDebuggerHelper.PARAMETERS_SEPARATOR)
        settings?.putUserData(GradleRunConfiguration.DEBUGGER_PARAMETERS_KEY, serializedParams)
        return super.executeTasks(id, taskNames, projectPath, settings, jvmParametersSetup, listener)
    }
}