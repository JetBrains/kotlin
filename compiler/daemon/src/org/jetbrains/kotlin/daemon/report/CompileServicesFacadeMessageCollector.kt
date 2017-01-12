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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.common.*

internal class CompileServicesFacadeMessageCollector(
        private val servicesFacade: CompilerServicesFacadeBase,
        compilationOptions: CompilationOptions
) : MessageCollector {
    private val mySeverity = compilationOptions.reportSeverity
    private var hasErrors = false

    override fun clear() {
        hasErrors = false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        val reportCategory = when (severity) {
            CompilerMessageSeverity.OUTPUT -> ReportCategory.OUTPUT_MESSAGE
            else -> ReportCategory.COMPILER_MESSAGE
        }

        val reportSeverity = when (severity) {
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.EXCEPTION -> ReportSeverity.ERROR
            CompilerMessageSeverity.WARNING -> ReportSeverity.WARNING
            CompilerMessageSeverity.INFO -> ReportSeverity.INFO
            CompilerMessageSeverity.LOGGING -> ReportSeverity.DEBUG
            CompilerMessageSeverity.OUTPUT -> ReportSeverity.ERROR
        }

        if (reportSeverity.code <= mySeverity) {
            servicesFacade.report(reportCategory, reportSeverity, message, CompilerMessageAttachment(severity, location))
        }

        hasErrors = hasErrors || severity == CompilerMessageSeverity.ERROR
    }

    override fun hasErrors(): Boolean = hasErrors
}
