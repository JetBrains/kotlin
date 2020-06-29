/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm

import com.intellij.execution.ExecutionTarget
import com.jetbrains.mobile.execution.Device
import com.jetbrains.mpp.BinaryTargetListener
import com.jetbrains.mpp.AppleRunConfiguration
import com.jetbrains.mpp.ProjectWorkspace

class KMMTargetListener(workspace: ProjectWorkspace) : BinaryTargetListener(workspace) {
    override fun activeTargetChanged(target: ExecutionTarget) {
        super.activeTargetChanged(target)

        (configuration() as? AppleRunConfiguration)?.let {
            it.selectedDevice = target as? Device
        }
    }
}