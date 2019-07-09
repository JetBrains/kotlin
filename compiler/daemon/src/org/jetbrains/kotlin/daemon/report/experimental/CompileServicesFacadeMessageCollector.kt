/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report.experimental

import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.daemon.KotlinCompileDaemon.log
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBaseAsync
import org.jetbrains.kotlin.daemon.common.report

internal class CompileServicesFacadeMessageCollector(
    private val servicesFacade: CompilerServicesFacadeBaseAsync,
    compilationOptions: CompilationOptions
) : MessageCollector {
    private val mySeverity = compilationOptions.reportSeverity
    private var hasErrors = false

    override fun clear() {
        hasErrors = false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        GlobalScope.async {
            log.info("Message: " + MessageRenderer.WITHOUT_PATHS.render(severity, message, location))
            when (severity) {
                CompilerMessageSeverity.OUTPUT -> {
                    servicesFacade.report(ReportCategory.OUTPUT_MESSAGE, ReportSeverity.ERROR, message)
                }
                CompilerMessageSeverity.EXCEPTION -> {
                    servicesFacade.report(ReportCategory.EXCEPTION, ReportSeverity.ERROR, message)
                }
                else -> {
                    val reportSeverity = when (severity) {
                        CompilerMessageSeverity.ERROR -> ReportSeverity.ERROR
                        CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> ReportSeverity.WARNING
                        CompilerMessageSeverity.INFO -> ReportSeverity.INFO
                        else -> ReportSeverity.DEBUG
                    }

                    if (reportSeverity.code <= mySeverity) {
                        servicesFacade.report(ReportCategory.COMPILER_MESSAGE, reportSeverity, message, location)
                    }
                }
            }

            hasErrors = hasErrors || severity.isError
        }
    }

    override fun hasErrors(): Boolean = hasErrors
}
