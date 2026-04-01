/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity

enum class Severity {
    INFO,
    ERROR,
    WARNING,

    /**
     * see [org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.FIXED_WARNING]
     */
    FIXED_WARNING,
    STRONG_WARNING;

    fun toCompilerMessageSeverity(): CompilerMessageSeverity = when (this) {
        INFO -> CompilerMessageSeverity.INFO
        ERROR -> CompilerMessageSeverity.ERROR
        WARNING -> CompilerMessageSeverity.WARNING
        STRONG_WARNING -> CompilerMessageSeverity.STRONG_WARNING
        FIXED_WARNING -> CompilerMessageSeverity.FIXED_WARNING
    }

    val isErrorWhenWError: Boolean
        get() = when (this) {
            INFO, ERROR -> false
            FIXED_WARNING -> false
            WARNING,
            STRONG_WARNING -> true
        }

    val isError: Boolean
        get() = when (this) {
            ERROR -> true
            INFO,
            WARNING,
            FIXED_WARNING,
            STRONG_WARNING -> false
        }
}
