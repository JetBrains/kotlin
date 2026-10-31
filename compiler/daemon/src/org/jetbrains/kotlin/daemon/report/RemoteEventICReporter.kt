/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.build.report.events.EventICReporter
import org.jetbrains.kotlin.daemon.common.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.CompilationResults

internal class RemoteEventICReporter(
    private val compilationResults: CompilationResults,
) : EventICReporter(), RemoteICReporter {
    override fun flush() {
        compilationResults.add(CompilationResultCategory.IC_EVENT.code, ArrayList(events))
    }
}
