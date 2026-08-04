/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.capture.BtaTypedEventCapture
import java.io.File

class LoggingICReporter(
    val projectId : String,
    val outputPath : String
)  : ICReporterBase(), RemoteICReporter {

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
        BtaTypedEventCapture.record(
            projectId,
            outputPath,
            severity.toString(),
            message()
        )
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        BtaTypedEventCapture.recordTypedEvent(
            projectId,
            outputPath,
            "COMPILE_ITERATION",
            "blabla",
            "abababa"
        )
    }

    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {
        BtaTypedEventCapture.recordTypedEvent(
            projectId,
            outputPath,
            "DIRTY_CLASS",
            "blublu",
            classFqName)
    }

    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {
        BtaTypedEventCapture.recordTypedEvent(
            projectId,
            outputPath,
            "DIRTY_MEMBER",
            "bleble",
            buildString {
                append("scope: ")
                append(scope)
                append(", name: ")
                append(name)
            }
        )
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        BtaTypedEventCapture.recordTypedEvent(
            projectId,
            outputPath,
            "DIRTY",
            "blabla",
            reason
        )
    }

    override fun flush() {
    }
}
