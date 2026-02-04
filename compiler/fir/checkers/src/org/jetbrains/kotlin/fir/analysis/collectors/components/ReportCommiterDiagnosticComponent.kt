/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.PendingDiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirFile

class ReportCommitterDiagnosticComponent(
    session: FirSession,
    reporter: PendingDiagnosticReporter
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitElement(element: FirElement, data: CheckerContext) {
        checkAndCommitReportsOn(element, data, commitEverything = false)
    }

    fun endOfFile(file: FirFile, context: CheckerContext) {
        checkAndCommitReportsOn(file, context, commitEverything = true)
    }
}
