/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionException
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.konan.WorkspaceXML
import com.jetbrains.mpp.debugger.GradleLLDBDriverConfiguration

@State(name = WorkspaceXML.projectComponentName, storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class MPPWorkspace(project: Project) : WorkspaceBase(project) {

    override val binaryRunConfigurationType = MPPBinaryRunConfigurationType::class.java
    override val lldbDriverConfiguration: LLDBDriverConfiguration by lazy {
        val home = lldbHome ?: throw ExecutionException("Debug is impossible without lldb binaries required by Kotlin/Native")
        GradleLLDBDriverConfiguration(home)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MPPWorkspace = project.getComponent(MPPWorkspace::class.java)
    }
}