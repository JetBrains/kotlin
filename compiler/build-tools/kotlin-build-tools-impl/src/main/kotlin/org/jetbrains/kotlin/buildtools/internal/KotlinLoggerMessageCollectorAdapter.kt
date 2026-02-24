/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

internal class KotlinLoggerMessageCollectorAdapter(
    internal val kotlinLogger: KotlinLogger,
    compilerMessageRenderer: CompilerMessageRenderer,
    private val warningsAsErrors: Boolean,
) : MessageCollector {

    private val messageRenderer = compilerMessageRenderer.asMessageRenderer()

    override fun clear() {}

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val effectiveSeverity = severity.toEffectiveSeverity(warningsAsErrors)
        val renderedMessage = messageRenderer.render(effectiveSeverity, message, location)

        when (effectiveSeverity) {
            CompilerMessageSeverity.EXCEPTION -> kotlinLogger.error(
                renderedMessage,
                RuntimeException(message)
            ) // TODO: get the original exception properly and avoid duplication of stacktrace in message
            CompilerMessageSeverity.ERROR -> kotlinLogger.error(renderedMessage)
            CompilerMessageSeverity.STRONG_WARNING, CompilerMessageSeverity.WARNING, CompilerMessageSeverity.FIXED_WARNING -> kotlinLogger.warn(
                renderedMessage
            )
            CompilerMessageSeverity.INFO -> kotlinLogger.info(renderedMessage)
            CompilerMessageSeverity.OUTPUT, CompilerMessageSeverity.LOGGING -> kotlinLogger.debug(renderedMessage)
        }
    }

    private fun CompilerMessageSeverity.toEffectiveSeverity(warningsAsErrors: Boolean) = when (this) {
        CompilerMessageSeverity.WARNING if warningsAsErrors -> CompilerMessageSeverity.ERROR
        CompilerMessageSeverity.STRONG_WARNING if warningsAsErrors -> CompilerMessageSeverity.ERROR
        // Explicitly listing all remaining severities instead of using `else` so that the compiler
        // forces a revisit here when new severity is added to CompilerMessageSeverity.
        CompilerMessageSeverity.OUTPUT,
        CompilerMessageSeverity.LOGGING,
        CompilerMessageSeverity.INFO,
        CompilerMessageSeverity.EXCEPTION,
        CompilerMessageSeverity.ERROR,
        CompilerMessageSeverity.WARNING,
        CompilerMessageSeverity.STRONG_WARNING,
        CompilerMessageSeverity.FIXED_WARNING,
            -> this
    }

    override fun hasErrors() = false
}
