/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.registerAllComponents
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.psi.KtElement

internal abstract class AbstractFirIdeDiagnosticsCollector(
    session: FirSession,
) : AbstractDiagnosticCollector(
    session,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        session,
        ScopeSession(),
        ImplicitBodyResolveComputationSession(),
        ::FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
    )
) {
    init {
        registerAllComponents()
    }

    protected abstract fun onDiagnostic(diagnostic: Diagnostic)


    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: FirDiagnostic<*>?) {
            if (diagnostic !is FirPsiDiagnostic<*>) return
            if (diagnostic.element.psi !is KtElement) return
            onDiagnostic(diagnostic)
        }
    }

    override var reporter: DiagnosticReporter = Reporter()

    override fun initializeCollector() {
        reporter = Reporter()
    }

    override fun beforeCollecting() {
        checkCanceled()
    }

    override fun getCollectedDiagnostics(): List<FirDiagnostic<*>> {
        // Not necessary in IDE
        return emptyList()
    }

    companion object {
        private val LOG = Logger.getInstance(AbstractFirIdeDiagnosticsCollector::class.java)
    }
}
