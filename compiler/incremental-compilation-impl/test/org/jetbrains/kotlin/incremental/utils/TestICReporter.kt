/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental.utils

import org.jetbrains.kotlin.build.report.ICReporter.ReportSeverity
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.build.report.ICReporterBase
import java.io.File

class TestICReporter : ICReporterBase() {
    private val compiledSourcesMutable = arrayListOf<File>()

    val compiledSources: List<File>
        get() = compiledSourcesMutable

    var exitCode: ExitCode = ExitCode.OK
        private set

    override fun report(message: () -> String, severity: ReportSeverity) {}

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        compiledSourcesMutable.addAll(sourceFiles)
        this.exitCode = exitCode
    }

    var cachesDump: String = ""
}