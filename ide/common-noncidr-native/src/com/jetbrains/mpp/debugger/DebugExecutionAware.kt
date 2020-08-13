package com.jetbrains.mpp.debugger

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemExecutionAware
import com.intellij.openapi.externalSystem.service.internal.AbstractExternalSystemTask
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager
import com.intellij.openapi.project.Project
import com.jetbrains.konan.KonanLog
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

abstract class DebugExecutionAware : ExternalSystemExecutionAware {

    abstract fun getParams(project: Project): Map<String, String>

    override fun prepareExecution(
        taskId: ExternalSystemTaskId,
        externalProjectPath: String,
        isPreviewMode: Boolean,
        taskNotificationListener: ExternalSystemTaskNotificationListener,
        project: Project
    ) {
        val task = ServiceManager.getService(ExternalSystemProcessingManager::class.java).findTask(
            taskId.type, GradleConstants.SYSTEM_ID, externalProjectPath
        ) as? AbstractExternalSystemTask

        if (task == null) {
            KonanLog.LOG.error("Could not setup debug for task id=$taskId")
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