/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.report
import java.io.File

@VisibleForTesting
class DebugMessagesICReporter(
    private val servicesFacade: CompilerServicesFacadeBase,
    rootDir: File?,
    private val reportSeverity: ICReporter.ReportSeverity
) : ICReporterBase(rootDir), RemoteICReporter {

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
        if (severity.level < reportSeverity.level) return

        servicesFacade.report(ReportCategory.IC_MESSAGE, severity.getSeverity(), message())
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
    }

    override fun flush() {
    }
}

internal fun ICReporter.ReportSeverity.getSeverity(): ReportSeverity {
    return when (this) {
        ICReporter.ReportSeverity.WARNING -> ReportSeverity.WARNING
        ICReporter.ReportSeverity.INFO -> ReportSeverity.INFO
        ICReporter.ReportSeverity.DEBUG -> ReportSeverity.DEBUG
    }
}
