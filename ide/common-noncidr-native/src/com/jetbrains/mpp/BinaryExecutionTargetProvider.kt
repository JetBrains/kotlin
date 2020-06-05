/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionTargetProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class BinaryExecutionTargetProvider : ExecutionTargetProvider() {
    override fun getTargets(project: Project, ideConfiguration: RunConfiguration): List<BinaryExecutionTarget> {
        val konanConfiguration = ideConfiguration as? BinaryRunConfigurationBase ?: return emptyList()
        return konanConfiguration.executable?.executionTargets ?: emptyList()
    }
}