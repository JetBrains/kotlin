/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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