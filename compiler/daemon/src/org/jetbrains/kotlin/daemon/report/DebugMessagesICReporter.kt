/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.RemoteICReporter
import java.io.File

internal class DebugMessagesICReporter(
    private val servicesFacade: CompilerServicesFacadeBase,
    rootDir: File,
    private val isVerbose: Boolean
) : ICReporterBase(rootDir), RemoteICReporter {
    override fun report(message: () -> String) {
        servicesFacade.report(
            ReportCategory.IC_MESSAGE,
            ReportSeverity.DEBUG, message()
        )
    }

    override fun reportVerbose(message: () -> String) {
        if (isVerbose) {
            report(message)
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
    }

    override fun flush() {
    }
}