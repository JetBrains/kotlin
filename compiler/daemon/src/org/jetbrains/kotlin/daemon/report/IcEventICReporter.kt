/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.build.report.ICReporter.ReportSeverity
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.build.report.io.IcEvent
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.CompilationResults
import java.io.File

@VisibleForTesting
class IcEventICReporter(
    private val compilationResults: CompilationResults,
    rootDir: File?,
) : ICReporterBase(rootDir), RemoteICReporter {
    private val icEvents = arrayListOf<IcEvent>()

    override fun report(message: () -> String, severity: ReportSeverity) {
    }

    override fun reportIcEvent(event: IcEvent) {
        icEvents.add(event)
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
    }

    override fun flush() {
        compilationResults.add(CompilationResultCategory.IC_EVENT.code, icEvents)
    }
}
