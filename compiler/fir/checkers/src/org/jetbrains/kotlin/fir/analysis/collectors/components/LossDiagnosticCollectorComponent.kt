/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.resolve.calls.InferenceError
import org.jetbrains.kotlin.fir.resolve.calls.InferredEmptyIntersectionDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSingleCandidate
import org.jetbrains.kotlin.resolve.calls.inference.model.InferredEmptyIntersectionWarning

class LossDiagnosticCollectorComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitElement(element: FirElement, data: CheckerContext) {
        if (element !is FirDiagnosticHolder || data.suppressedDiagnostics.isNotEmpty()) {
            return
        }

        if (reporter !is PendingDiagnosticsCollectorWithSuppress) {
            return
        }

        val source = element.source
        val diagnostic = element.diagnostic
        if (diagnostic is ConeDiagnosticWithSingleCandidate) {
            if (diagnostic.candidate.diagnostics.all {
                    when (it) {
                        is InferredEmptyIntersectionDiagnostic -> !it.isError
                        is InferenceError -> it.constraintError is InferredEmptyIntersectionWarning
                        else -> false
                    }
                }
            ) {
                return
            }
        }
        reporter.reportOn(source, FirErrors.OTHER_ERROR_WITH_REASON, diagnostic.reason, data)
    }
}
