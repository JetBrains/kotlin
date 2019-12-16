/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File

internal class CompositeICReporter(private val reporters: Iterable<RemoteICReporter>) :
    RemoteICReporter {
    override fun report(message: () -> String) {
        reporters.forEach { it.report(message) }
    }

    override fun reportVerbose(message: () -> String) {
        reporters.forEach { it.reportVerbose(message) }
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

    override fun startMeasure(metric: String, startNs: Long) {
        reporters.forEach { it.startMeasure(metric, startNs) }
    }

    override fun endMeasure(metric: String, endNs: Long) {
        reporters.forEach { it.endMeasure(metric, endNs) }
    }

    override fun flush() {
        reporters.forEach { it.flush() }
    }
}