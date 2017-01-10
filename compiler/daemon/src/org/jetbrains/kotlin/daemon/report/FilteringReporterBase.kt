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

import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.ReportCategory
import java.io.Serializable

internal open class FilteringReporterBase(
        private val servicesFacade: CompilerServicesFacadeBase,
        additionalCompilerArgs: CompilationOptions,
        private val reportCategory: ReportCategory
) {
    private val reportingFilter = additionalCompilerArgs.reportingFilters.firstOrNull { it.category == reportCategory }

    protected fun report(severity: Int, message: String?, attachment: Serializable? = null) {
        if (shouldReport(severity)) {
            servicesFacade.report(reportCategory, severity, message, attachment)
        }
    }

    protected fun shouldReport(severity: Int): Boolean =
        reportingFilter == null || severity in reportingFilter.severities
}