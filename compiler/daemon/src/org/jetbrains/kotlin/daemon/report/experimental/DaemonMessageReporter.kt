/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report.experimental

import org.jetbrains.kotlin.daemon.common.experimental.*
import java.io.PrintStream

internal interface DaemonMessageReporter {
    fun report(severity: ReportSeverity, message: String)
}

internal fun DaemonMessageReporter(
        servicesFacade: CompilerServicesFacadeBase,
        compilationOptions: CompilationOptions
): DaemonMessageReporter =
        if (ReportCategory.DAEMON_MESSAGE.code in compilationOptions.reportCategories) {
            val mySeverity = ReportSeverity.fromCode(compilationOptions.reportSeverity)!!
            DaemonMessageReporterImpl(servicesFacade, mySeverity)
        }
        else {
            DummyDaemonMessageReporter
        }

internal class DaemonMessageReporterPrintStreamAdapter(private val out: PrintStream): DaemonMessageReporter {
    override fun report(severity: ReportSeverity, message: String) {
        out.print("[Kotlin compile daemon][$severity] $message")
    }
}

private class DaemonMessageReporterImpl(
        private val servicesFacade: CompilerServicesFacadeBase,
        private val mySeverity: ReportSeverity
): DaemonMessageReporter {
    override fun report(severity: ReportSeverity, message: String) {
        if (severity.code <= mySeverity.code) {
            servicesFacade.report(ReportCategory.DAEMON_MESSAGE.code, severity.code, message, attachment = null)
        }
    }
}

private object DummyDaemonMessageReporter : DaemonMessageReporter {
    override fun report(severity: ReportSeverity, message: String) {
    }
}