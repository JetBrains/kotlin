/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.messages

import java.util.*

enum class CompilerMessageSeverity {
    EXCEPTION,
    ERROR,

    /**
     * Unlike a normal warning, a strong warning is not discarded when there are compilation errors.
     * Use it for problems related to configuration, not the diagnostics.
     */
    STRONG_WARNING,
    WARNING,
    INFO,
    LOGGING,

    /**
     * Source to output files mapping messages (e.g A.kt->A.class).
     * It is needed for incremental compilation.
     */
    OUTPUT;

    val isError: Boolean
        get() = this == EXCEPTION || this == ERROR

    val isWarning: Boolean
        get() = this == STRONG_WARNING || this == WARNING

    val presentableName: String
        get() = when (this) {
            EXCEPTION -> "exception"
            ERROR -> "error"
            STRONG_WARNING, WARNING -> "warning"
            INFO -> "info"
            LOGGING -> "logging"
            OUTPUT -> "output"
        }

    companion object {
        @JvmField
        val VERBOSE: EnumSet<CompilerMessageSeverity> = EnumSet.of(LOGGING)
    }
}
