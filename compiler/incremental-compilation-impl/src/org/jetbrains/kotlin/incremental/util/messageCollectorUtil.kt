/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.incremental.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector


internal fun MessageCollector.reportException(e: Throwable) {
    report(severity = CompilerMessageSeverity.EXCEPTION, message = "Incremental compilation failed:${e.message}\n${e.stackTraceToString()}")
}

