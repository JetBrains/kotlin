/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetListener
import com.intellij.execution.RunManager

open class BinaryTargetListener(private val workspace: WorkspaceBase) :
    ExecutionTargetListener {
    protected fun configuration() = RunManager.getInstance(workspace.project).selectedConfiguration?.configuration

    override fun activeTargetChanged(target: ExecutionTarget) {
        (configuration() as? BinaryRunConfigurationBase)?.let {
            it.selectedTarget = target as? BinaryExecutionTarget
        }
    }
}