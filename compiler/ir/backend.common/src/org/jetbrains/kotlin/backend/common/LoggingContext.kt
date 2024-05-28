/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity

interface LoggingContext : ErrorReportingContext {

    /**
     * Whether logging is enabled for the current [CompilerPhase] by the `-Xverbose-phases` CLI option.
     */
    var inVerbosePhase: Boolean

    /**
     * If [inVerbosePhase] is `true`, prints [message] to the [messageCollector].
     */
    fun log(message: () -> String) {
        if (inVerbosePhase) {
            // Not CompilerMessageSeverity.LOGGING, because that's only printed if the `-verbose` flag is passed.
            messageCollector.report(CompilerMessageSeverity.INFO, message())
        }
    }
}
