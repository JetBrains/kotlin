/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

/**
 * An adapter for the [Logger] interface that reports all messages to compiler's [MessageCollector].
 */
private class MessageCollectorAdapter(private val messageCollector: MessageCollector) : Logger {
    override fun log(message: String) = messageCollector.report(CompilerMessageSeverity.INFO, message, null)
    override fun warning(message: String) = messageCollector.report(CompilerMessageSeverity.WARNING, message, null)
    override fun strongWarning(message: String) = messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, message, null)
    override fun error(message: String) = messageCollector.report(CompilerMessageSeverity.ERROR, message, null)

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String): Nothing {
        error(message)
        throw CompilationErrorException()
    }
}

fun MessageCollector.toLogger(): Logger {
    return if (this != MessageCollector.NONE) MessageCollectorAdapter(this) else DummyLogger
}
