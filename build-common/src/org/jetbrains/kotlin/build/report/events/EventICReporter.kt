/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.events

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import java.io.File

open class EventICReporter : ICReporter {
    private val fqNameReasons = mutableMapOf<String, MutableSet<String>>()
    private val symbolReasons = mutableMapOf<Pair<String, String>, MutableSet<String>>()
    private val fileReasons = mutableMapOf<String, MutableSet<String>>()

    private var iteration = 0

    val events: List<IcEventImpl>
        field = arrayListOf<IcEventImpl>()

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {}

    override fun reportCompilationStart(isIncremental: Boolean, reason: String?) {
        events.add(IcEventImpl.CompilationStarted(isIncremental, reason, 0))
        iteration = 0
        if (!isIncremental) {
            fileReasons.clear()
            fqNameReasons.clear()
            symbolReasons.clear()
        }
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

    override fun reportProcessedChanges(
        dirtySymbols: Iterable<Pair<String, String>>,
        dirtyClasses: Iterable<String>,
        dirtySealed: Iterable<String>
    ) {
        dirtySymbols.forEach {
            symbolReasons.getOrPut(it) { mutableSetOf() }.add(
                if (it.second == SAM_LOOKUP_NAME.asString()) ScopeExpansionReason.usesInSamConversion(it.first)
                else ScopeExpansionReason.usesAffectedSymbol(it.first, it.second)
            )
        }
        dirtyClasses.forEach {
            fqNameReasons.getOrPut(it) { mutableSetOf() }.add(ScopeExpansionReason.declaresAffectedClass(it))
        }
        dirtySealed.forEach {
            fqNameReasons.getOrPut(it) { mutableSetOf() }.add(ScopeExpansionReason.declaresSealedClass(it))
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        val fallback = listOf(if (incremental) ScopeExpansionReason.UNKNOWN.toString() else ScopeExpansionReason.REBUILD.toString())
        val reasons = sourceFiles.associate { file -> file.path to (fileReasons[file.path]?.takeIf { incremental }?.toList() ?: fallback) }
        events.add(IcEventImpl.CompileIteration(reasons, exitCode.toString(), iteration++))
        fileReasons.clear()
        fqNameReasons.clear()
        symbolReasons.clear()
    }
}
