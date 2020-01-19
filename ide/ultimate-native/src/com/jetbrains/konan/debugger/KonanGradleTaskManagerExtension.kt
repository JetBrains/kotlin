/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.konan.IdeaKonanWorkspace
import com.jetbrains.konan.debugger.GradleLLDBDebuggerBackend.Companion.DEBUG_SERVER_ARGS_KEY
import com.jetbrains.konan.debugger.GradleLLDBDebuggerBackend.Companion.DEBUG_SERVER_PATH_KEY
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings

class KonanGradleTaskManagerExtension : GradleTaskManagerExtension {
    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) = true

    override fun executeTasks(
        id: ExternalSystemTaskId,
        taskNames: MutableList<String>,
        projectPath: String,
        settings: GradleExecutionSettings?,
        jvmParametersSetup: String?,
        listener: ExternalSystemTaskNotificationListener
    ): Boolean {
        val project = ProjectManager.getInstance().openProjects.firstOrNull { it.basePath == projectPath } ?: return false
        val workspace = IdeaKonanWorkspace.getInstance(project)

        if (HostManager.host == KonanTarget.MINGW_X64 || workspace.lldbHome == null) {
            return super.executeTasks(id, taskNames, projectPath, settings, jvmParametersSetup, listener)
        }

        val params = HashMap<String, String>()

        var debugServerPath = ""

        when (HostManager.host) {
            KonanTarget.MACOS_X64 -> debugServerPath = "LLDB.framework/Resources/debugserver"
            KonanTarget.LINUX_X64 -> {
                debugServerPath = "bin/lldb-server"
                params[DEBUG_SERVER_ARGS_KEY] = "g"
            }
        }

        params[DEBUG_SERVER_PATH_KEY] = workspace.lldbHome!!.resolve(debugServerPath).toString()

        val serializedParams = params.map { (key, value) -> "$key=$value" }.joinToString(separator = ForkedDebuggerHelper.PARAMETERS_SEPARATOR)

        settings?.putUserData(GradleRunConfiguration.DEBUGGER_PARAMETERS_KEY, serializedParams)
        return super.executeTasks(id, taskNames, projectPath, settings, jvmParametersSetup, listener)
    }
}