/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.SourceLocation
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KotlinLoggerMessageCollectorAdapterTest {
    @Test
    fun exceptionRoutesToErrorWithThrowable() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.EXCEPTION, "crash", null)

        val call = logger.calls.single()
        assertEquals(LogMethod.ERROR_WITH_THROWABLE, call.method)
        assertEquals("crash", call.message)
        assertIs<RuntimeException>(call.throwable)
    }

    @Test
    fun errorRoutesToError() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.ERROR, "it is an error!", null)

        assertEquals(LogMethod.ERROR, logger.calls.single().method)
    }

    @Test
    fun warningRoutesToWarn() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.WARNING, "simple warning", null)

        assertEquals(LogMethod.WARN, logger.calls.single().method)
    }

    @Test
    fun strongWarningRoutesToWarn() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.STRONG_WARNING, "strong warning", null)

        assertEquals(LogMethod.WARN, logger.calls.single().method)
    }

    @Test
    fun fixedWarningRoutesToWarn() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.FIXED_WARNING, "fixed warning", null)

        assertEquals(LogMethod.WARN, logger.calls.single().method)
    }

    @Test
    fun infoRoutesToInfo() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.INFO, "info message", null)

        assertEquals(LogMethod.INFO, logger.calls.single().method)
    }

    @Test
    fun outputRoutesToDebug() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.OUTPUT, "some outputs", null)

        assertEquals(LogMethod.DEBUG, logger.calls.single().method)
    }

    @Test
    fun loggingRoutesToDebug() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.LOGGING, "log", null)

        assertEquals(LogMethod.DEBUG, logger.calls.single().method)
    }

    @Test
    fun warningsAsErrorsPromotesWarningToError() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = true)

        adapter.report(CompilerMessageSeverity.WARNING, "warn", null)

        assertEquals(LogMethod.ERROR, logger.calls.single().method)
    }

    @Test
    fun warningsAsErrorsPromotesStrongWarningToError() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = true)

        adapter.report(CompilerMessageSeverity.STRONG_WARNING, "strong warn", null)

        assertEquals(LogMethod.ERROR, logger.calls.single().method)
    }

    @Test
    fun warningsAsErrorsDoesNotPromoteFixedWarning() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = true)

        adapter.report(CompilerMessageSeverity.FIXED_WARNING, "fixed warn", null)

        assertEquals(LogMethod.WARN, logger.calls.single().method)
    }

    @Test
    fun warningsAsErrorsFalseReturnsWarning() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.WARNING, "warn", null)
        adapter.report(CompilerMessageSeverity.STRONG_WARNING, "strong warn", null)

        assertEquals(
            listOf(LogMethod.WARN, LogMethod.WARN),
            logger.calls.map { it.method }
        )
    }

    @Test
    fun warningsAsErrorsDoesNotPromoteNonWarnings() {
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, DefaultCompilerMessageRenderer, warningsAsErrors = true)

        adapter.report(CompilerMessageSeverity.EXCEPTION, "ex", null)
        adapter.report(CompilerMessageSeverity.FIXED_WARNING, "fw", null)
        adapter.report(CompilerMessageSeverity.INFO, "i", null)
        adapter.report(CompilerMessageSeverity.LOGGING, "l", null)
        adapter.report(CompilerMessageSeverity.OUTPUT, "o", null)

        assertEquals(
            listOf(LogMethod.ERROR_WITH_THROWABLE, LogMethod.WARN, LogMethod.INFO, LogMethod.DEBUG, LogMethod.DEBUG),
            logger.calls.map { it.method }
        )
    }

    @Test
    fun renderedMessageIsPassedToLogger() {
        val prefixRenderer = object : CompilerMessageRenderer {
            override fun render(severity: Severity, message: String, location: SourceLocation?): String = "[PREFIX] $message"
        }
        val logger = CapturingLogger()
        val adapter = KotlinLoggerMessageCollectorAdapter(logger, prefixRenderer, warningsAsErrors = false)

        adapter.report(CompilerMessageSeverity.WARNING, "msg", null)

        assertEquals("[PREFIX] msg", logger.calls.single().message)
    }

    private enum class LogMethod { ERROR, ERROR_WITH_THROWABLE, WARN, INFO, DEBUG }

    private data class LogCall(
        val method: LogMethod,
        val message: String?,
        val throwable: Throwable? = null,
    )

    private class CapturingLogger : KotlinLogger {
        override val isDebugEnabled = true
        val calls = mutableListOf<LogCall>()

        override fun debug(msg: String) {
            calls.add(LogCall(LogMethod.DEBUG, msg))
        }

        override fun error(msg: String, throwable: Throwable?) {
            if (throwable != null) {
                calls.add(LogCall(LogMethod.ERROR_WITH_THROWABLE, msg, throwable))
            } else {
                calls.add(LogCall(LogMethod.ERROR, msg))
            }
        }

        override fun info(msg: String) {
            calls.add(LogCall(LogMethod.INFO, msg))
        }

        override fun lifecycle(msg: String) {}

        override fun warn(msg: String, throwable: Throwable?) {
            calls.add(LogCall(LogMethod.WARN, msg))
        }
    }
}
