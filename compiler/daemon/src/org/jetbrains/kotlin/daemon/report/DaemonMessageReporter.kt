/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.daemon.common.*
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