/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Severity.*
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

/**
 * An adapter for the [Logger] interface that reports all messages to compiler's [IrMessageLogger].
 */
private class IrMessageLoggerAdapter(private val irMessageLogger: IrMessageLogger) : Logger {
    override fun log(message: String) = irMessageLogger.report(INFO, message, null)
    override fun warning(message: String) = irMessageLogger.report(WARNING, message, null)
    override fun error(message: String) = irMessageLogger.report(ERROR, message, null)

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String): Nothing {
        error(message)
        throw CompilationErrorException()
    }
}

fun IrMessageLogger.toLogger(): Logger {
    return if (this != IrMessageLogger.None) IrMessageLoggerAdapter(this) else DummyLogger
}
