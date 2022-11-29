/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.incremental.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

public fun MessageCollector.reportException(e: Throwable, location: ExceptionLocation) {
    report(severity = CompilerMessageSeverity.EXCEPTION, message = "${location.readableName} failed: ${e.message}\n${e.stackTraceToString()}")
}

public enum class ExceptionLocation(val readableName: String) {
    INCREMENTAL_COMPILATION("Incremental compilation"),
    DAEMON("Daemon compilation"),
    OUT_OF_PROCESS_COMPILATION("Out of process compilation")
}

