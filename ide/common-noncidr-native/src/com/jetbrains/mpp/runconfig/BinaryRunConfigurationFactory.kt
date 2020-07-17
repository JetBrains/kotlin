/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.WorkspaceBase

class BinaryRunConfigurationFactory(
    type: ConfigurationType,
    private val getWorkspace: (Project) -> WorkspaceBase
) : ConfigurationFactory(type) {
    override fun getId() = "BinaryRunConfigurationFactory_" + type.id

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        BinaryRunConfiguration(getWorkspace(project), project, this)
}