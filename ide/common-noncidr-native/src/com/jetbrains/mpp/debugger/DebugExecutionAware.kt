package com.jetbrains.mpp.debugger

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemExecutionAware
import com.intellij.openapi.externalSystem.service.internal.AbstractExternalSystemTask
import com.intellij.openapi.project.Project
import com.jetbrains.konan.KonanLog
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

abstract class DebugExecutionAware : ExternalSystemExecutionAware {

    abstract fun getParams(project: Project): Map<String, String>

    override fun prepareExecution(
        taskRaw: ExternalSystemTask,
        externalProjectPath: String,
        isPreviewMode: Boolean,
        taskNotificationListener: ExternalSystemTaskNotificationListener,
        project: Project
    ) {
        val task = taskRaw as? AbstractExternalSystemTask

        if (task == null) {
            KonanLog.LOG.error("Could not setup debug for task=$task")
            return
        }
        val serializedParams = getParams(project)
            .map { (key, value) -> "$key=$value" }
            .joinToString(separator = ForkedDebuggerHelper.PARAMETERS_SEPARATOR)

        task.putUserData(GradleRunConfiguration.DEBUGGER_PARAMETERS_KEY, serializedParams)
    }

    companion object {
        const val LOCAL_DEBUG_SERVER = "LLDB.framework/Resources/debugserver"
    }
}