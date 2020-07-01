package com.jetbrains.mobile.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class DeviceExecutionTargetProvider : ExecutionTargetProvider() {
    override fun getTargets(project: Project, configuration: RunConfiguration): List<ExecutionTarget> =
        if (configuration is MobileRunConfiguration)
            DeviceService.getInstance(project).getAll()
        else emptyList()
}