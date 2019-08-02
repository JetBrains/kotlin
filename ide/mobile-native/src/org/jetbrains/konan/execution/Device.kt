/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import org.jetbrains.konan.gradle.execution.GradleKonanBuildProfileExecutionTarget

abstract class Device(
    id: String,
    val name: String,
    val osName: String,
    val osVersion: String
) : GradleKonanBuildProfileExecutionTarget(id) {
    override fun getDisplayName(): String = "$name | $osName $osVersion"
}