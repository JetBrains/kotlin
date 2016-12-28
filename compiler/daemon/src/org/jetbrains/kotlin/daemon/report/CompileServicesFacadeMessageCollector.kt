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
import org.jetbrains.kotlin.daemon.common.AdditionalCompilerArguments
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.ReportCategory

internal class CompileServicesFacadeMessageCollector(
        private val servicesFacade: CompilerServicesFacadeBase,
        additionalCompilerArguments: AdditionalCompilerArguments
) : MessageCollector {
    private var hasErrors = false
    private val reportingFilter = additionalCompilerArguments.reportingFilters.firstOrNull { it.category == ReportCategory.DAEMON_MESSAGE }

    override fun clear() {
        hasErrors = false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        if (reportingFilter != null && severity.value !in reportingFilter.severities) return

        hasErrors = hasErrors || severity == CompilerMessageSeverity.ERROR
        servicesFacade.report(ReportCategory.DAEMON_MESSAGE, severity.value, message, location)
    }

    override fun hasErrors(): Boolean = hasErrors
}