/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.BaseDiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.DiagnosticReporterWithSuppress
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.SimpleDiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve

class SimpleDiagnosticsCollector(
    session: FirSession,
    scopeSession: ScopeSession,
    createComponents: (DiagnosticReporter) -> List<AbstractDiagnosticCollectorComponent>,
) : AbstractDiagnosticCollector(session, scopeSession, createComponents) {
    override fun createVisitor(components: List<AbstractDiagnosticCollectorComponent>): CheckerRunningDiagnosticCollectorVisitor {
        return CheckerRunningDiagnosticCollectorVisitor(
            PersistentCheckerContext(
                this,
                ReturnTypeCalculatorForFullBodyResolve()
            ),
            components
        )
    }
}

