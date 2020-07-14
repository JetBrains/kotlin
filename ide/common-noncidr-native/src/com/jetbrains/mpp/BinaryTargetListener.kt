/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetListener
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration

internal class BinaryTargetListener(private val project: Project) : ExecutionTargetListener {

    override fun activeTargetChanged(target: ExecutionTarget) {
        val runManager = RunManager.getInstance(project)
        val selectedConfiguration = runManager.selectedConfiguration?.configuration as? BinaryRunConfiguration

        selectedConfiguration?.selectedTarget = target as? BinaryExecutionTarget
    }
}