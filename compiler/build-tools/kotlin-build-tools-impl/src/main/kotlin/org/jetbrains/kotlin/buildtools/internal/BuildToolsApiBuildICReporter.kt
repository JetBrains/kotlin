/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompileIterationResult
import java.io.File

internal class BuildToolsApiBuildICReporter(
    private val kotlinLogger: KotlinLogger,
    private val rootProjectDir: File?,
) : ICReporterBase() {
    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
        when (severity) {
            ICReporter.ReportSeverity.DEBUG -> if (kotlinLogger.isDebugEnabled) {
                kotlinLogger.debug(message())
            }
            ICReporter.ReportSeverity.INFO -> kotlinLogger.info(message())
            ICReporter.ReportSeverity.WARNING -> kotlinLogger.warn(message())
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        kotlinLogger.debug(CompileIterationResult(sourceFiles, exitCode.toString()), rootProjectDir)
    }
}