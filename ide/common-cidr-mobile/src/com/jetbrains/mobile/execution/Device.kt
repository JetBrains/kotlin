/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.configurations.RunConfiguration
import javax.swing.Icon

abstract class Device(
    private val uniqueID: String,
    val name: String,
    val osName: String,
    val osVersion: String
) : ExecutionTarget() {
    override fun getId(): String = uniqueID
    override fun getDisplayName(): String = "$name | $osName $osVersion"
    override fun getIcon(): Icon? = null

    override fun canRun(configuration: RunConfiguration): Boolean =
        configuration is MobileRunConfiguration
}