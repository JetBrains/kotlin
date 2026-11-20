/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import org.jetbrains.kotlin.build.report.ICReporter.ReportSeverity
import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File

open class CompositeICReporter(private val reporters: Iterable<ICReporter>) : ICReporter {
    override fun report(message: () -> String, severity: ReportSeverity) {
        reporters.forEach { it.report(message, severity) }
    }

    override fun reportCompilationStart(isIncremental: Boolean, reason: String?) {
        reporters.forEach { it.reportCompilationStart(isIncremental, reason) }
    }

    override fun reportProcessedChanges(
        dirtySymbols: Iterable<Pair<String, String>>,
        dirtyClasses: Iterable<String>,
        dirtySealed: Iterable<String>
    ) {
        reporters.forEach { it.reportProcessedChanges(dirtySymbols, dirtyClasses, dirtySealed) }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        reporters.forEach { it.reportCompileIteration(incremental, sourceFiles, exitCode) }
    }

    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {
        reporters.forEach { it.reportMarkDirtyClass(affectedFiles, classFqName) }
    }

    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {
        reporters.forEach { it.reportMarkDirtyMember(affectedFiles, scope, name) }
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        reporters.forEach { it.reportMarkDirty(affectedFiles, reason) }
    }
}
