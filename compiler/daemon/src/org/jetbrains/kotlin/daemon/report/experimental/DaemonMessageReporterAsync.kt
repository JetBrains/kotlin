/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report.experimental

import kotlinx.coroutines.experimental.async
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.experimental.CompilerServicesFacadeBaseAsync
import java.io.PrintStream

internal interface DaemonMessageReporterAsync {
    fun report(severity: ReportSeverity, message: String)
}

internal fun DaemonMessageReporterAsync(
    servicesFacade: CompilerServicesFacadeBaseAsync,
    compilationOptions: CompilationOptions
): DaemonMessageReporterAsync =
    if (ReportCategory.DAEMON_MESSAGE.code in compilationOptions.reportCategories) {
        val mySeverity = ReportSeverity.fromCode(compilationOptions.reportSeverity)!!
        DaemonMessageReporterAsyncAsyncImpl(servicesFacade, mySeverity)
    } else {
        DummyDaemonMessageReporterAsync
    }

internal class DaemonMessageReporterAsyncPrintStreamAdapter(private val out: PrintStream) : DaemonMessageReporterAsync {
    override fun report(severity: ReportSeverity, message: String) {
        out.print("[Kotlin compile daemon][$severity] $message")
    }
}

private class DaemonMessageReporterAsyncAsyncImpl(
    private val servicesFacade: CompilerServicesFacadeBaseAsync,
    private val mySeverity: ReportSeverity
) : DaemonMessageReporterAsync {
    override fun report(severity: ReportSeverity, message: String) {
        async {
            if (severity.code <= mySeverity.code) {
                servicesFacade.report(ReportCategory.DAEMON_MESSAGE.code, severity.code, message, attachment = null)
            }
        }
    }
}

private object DummyDaemonMessageReporterAsync : DaemonMessageReporterAsync {
    override fun report(severity: ReportSeverity, message: String) {
    }
}