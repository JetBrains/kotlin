/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

class DaemonJVMOptionsConfigurator(
    vararg val additionalParams: String,
    val inheritMemoryLimits: Boolean,
    val inheritOtherJvmOptions: Boolean,
    val inheritAdditionalProperties: Boolean
)