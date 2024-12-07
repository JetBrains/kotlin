/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

/**
 * Describes how IR verification errors should be reported when running the backend, if such verification is performed.
 */
enum class IrVerificationMode {

    /**
     * Verification of IR is disabled.
     */
    NONE,

    /**
     * Verification of IR is enabled; verification errors are reported as warnings, the backend pipeline is executed further.
     */
    WARNING,

    /**
     * Verification of IR is enabled; any verification errors abort the backend pipeline.
     */
    ERROR;

    companion object {
        fun resolveMode(key: String): IrVerificationMode? =
            entries.find { it.name.equals(key, ignoreCase = true) }
    }
}
