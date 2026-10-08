/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.build.report.ICReporter.ReportSeverity
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.build.report.events.IcEventImpl
import org.jetbrains.kotlin.build.report.events.ScopeExpansionReason
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.incremental.ChangeInfo
import java.io.File
import kotlin.collections.forEach

@VisibleForTesting
class EventICReporter(
    private val compilationResults: CompilationResults,
    rootDir: File?,
) : ICReporterBase(rootDir), RemoteICReporter {
    private val icEvents = arrayListOf<IcEventImpl>()
    private val pendingEvents = mutableListOf<IcEventImpl>()

    private val fqNameReasons = mutableMapOf<String, MutableSet<String>>()
    private val symbolReasons = mutableMapOf<Pair<String, String>, MutableSet<String>>()
    private val fileReasons = mutableMapOf<String, MutableSet<String>>()

    private var iteration = 0

    override fun report(message: () -> String, severity: ReportSeverity) {
    }

    override fun reportIcEvent(event: IcEventImpl) {
        pendingEvents.add(event)
    }

    override fun reportCompilationStart(isIncremental: Boolean, reason: String?) {
        pendingEvents.add(IcEventImpl.CompilationStarted(isIncremental, reason))
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        affectedFiles.forEach { fileReasons.getOrPut(it.path) { linkedSetOf() }.add(reason) }
    }

    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {
        for (file in affectedFiles) {
            fqNameReasons.getOrPut(classFqName) { mutableSetOf() }.forEach {
                fileReasons.getOrPut(file.path) { linkedSetOf() }.add(it)
            }
        }
    }

    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {
        for (file in affectedFiles) {
            symbolReasons.getOrPut(Pair(scope, name)) { mutableSetOf() }.forEach {
                fileReasons.getOrPut(file.path) { linkedSetOf() }.add(it)
            }
        }
    }

    override fun reportProcessedChanges(change: ChangeInfo, symbols: Iterable<Pair<String, String>>, fqnames: Iterable<String>) {
        fqnames.forEach {
            fqNameReasons.getOrPut(it) { mutableSetOf() }.add(change.toReadableString())
        }
        symbols.forEach {
            symbolReasons.getOrPut(it) { mutableSetOf() }.add(change.toReadableString())
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        // TODO: do we start iteration from 0, or does 0 represent "before doCompile loop" and first iteration is 1?
        pendingEvents.add(
            IcEventImpl.CompileIteration(
                sourceFiles.map { it.path },
//                fileReasons.filterKeys { it in sourceFiles.map { it.path } }.mapValues { it.value.toList() },
                fileReasons.mapValues { it.value.toList() },
                exitCode.toString()
            )
        )
        pendingEvents.forEach { it.iteration = iteration }
        icEvents.addAll(pendingEvents)
        pendingEvents.clear()
        fileReasons.clear()
        fqNameReasons.clear()
        symbolReasons.clear()
        iteration++
    }

    override fun flush() {
        icEvents.addAll(pendingEvents)
        compilationResults.add(CompilationResultCategory.IC_EVENT.code, icEvents)
    }


    companion object {
        fun ChangeInfo.toReadableString(): String = when (this) {
            is ChangeInfo.Removed -> ScopeExpansionReason.ABI_REMOVED
            is ChangeInfo.MembersChanged -> ScopeExpansionReason.ABI_MEMBERS_CHANGED
            is ChangeInfo.SignatureChanged -> ScopeExpansionReason.ABI_SIGNATURE_CHANGED
            is ChangeInfo.ParentsChanged -> ScopeExpansionReason.ABI_PARENTS_CHANGED
        }.readableString + this.fqName.asString()
    }
}
