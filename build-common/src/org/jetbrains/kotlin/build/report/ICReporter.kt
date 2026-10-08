/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import org.jetbrains.kotlin.build.report.events.IcEventImpl
import org.jetbrains.kotlin.build.report.events.ScopeExpansionReason
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.incremental.ChangeInfo
import java.io.File

interface ICReporter {

    enum class ReportSeverity(val level: Int) {
        WARNING(3),
        INFO(2),
        DEBUG(1);
    }

    fun report(message: () -> String, severity: ReportSeverity)
    fun reportIcEvent(event: IcEventImpl)

    fun reportCompilationStart(isIncremental: Boolean, reason: String?)
    fun reportProcessedChanges(change: ChangeInfo, symbols: Iterable<Pair<String, String>>, fqnames: Iterable<String>)

    fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode)
    fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String)
    fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String)
    fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String)
}

//TODO check and remove?
fun ICReporter.warn(message: () -> String) = report(message, severity = ICReporter.ReportSeverity.WARNING)
fun ICReporter.info(message: () -> String) = report(message, severity = ICReporter.ReportSeverity.INFO)
fun ICReporter.debug(message: () -> String) = report(message, severity = ICReporter.ReportSeverity.DEBUG)

fun ICReporter.reportMarkDirty(affectedFiles: Iterable<File>, reason: ScopeExpansionReason) =
    reportMarkDirty(affectedFiles, reason.readableString)

object DoNothingICReporter : ICReporter {
    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {}
    override fun reportIcEvent(event: IcEventImpl) {}
    override fun reportCompilationStart(isIncremental: Boolean, reason: String?) {}
    override fun reportProcessedChanges(change: ChangeInfo, symbols: Iterable<Pair<String, String>>, fqnames: Iterable<String>) {}
    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {}
    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {}
    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {}
    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {}
}

