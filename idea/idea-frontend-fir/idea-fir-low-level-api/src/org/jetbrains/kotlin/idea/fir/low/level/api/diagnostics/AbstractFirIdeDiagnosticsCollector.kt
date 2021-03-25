/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.checkers.CommonDeclarationCheckers
import org.jetbrains.kotlin.fir.checkers.CommonExpressionCheckers
import org.jetbrains.kotlin.fir.checkers.ExtendedDeclarationCheckers
import org.jetbrains.kotlin.fir.checkers.ExtendedExpressionCheckers
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled

internal abstract class AbstractFirIdeDiagnosticsCollector(
    session: FirSession,
    useExtendedCheckers: Boolean,
) : AbstractDiagnosticCollector(
    session
) {
    init {
        val declarationCheckers = CheckersFactory.createDeclarationCheckers(useExtendedCheckers)
        val expressionCheckers = CheckersFactory.createExpressionCheckers(useExtendedCheckers)

        @Suppress("LeakingThis")
        initializeComponents(
            DeclarationCheckersDiagnosticComponent(this, declarationCheckers),
            ExpressionCheckersDiagnosticComponent(this, expressionCheckers),
            ErrorNodeDiagnosticCollectorComponent(this),
            ControlFlowAnalysisDiagnosticComponent(this, declarationCheckers),
        )
    }

    protected abstract fun onDiagnostic(diagnostic: FirPsiDiagnostic<*>)


    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: FirDiagnostic<*>?, context: CheckerContext) {
            if (diagnostic !is FirPsiDiagnostic<*>) return
            if (context.isDiagnosticSuppressed(diagnostic)) return
            onDiagnostic(diagnostic)
        }
    }

    override var reporter: DiagnosticReporter = Reporter()

    override fun initializeCollector() {
        reporter = Reporter()
    }


    override fun getCollectedDiagnostics(): List<FirDiagnostic<*>> {
        // Not necessary in IDE
        return emptyList()
    }
}


private object CheckersFactory {
    private val extendedDeclarationCheckers = createDeclarationCheckers(ExtendedDeclarationCheckers)
    private val commonDeclarationCheckers = createDeclarationCheckers(CommonDeclarationCheckers)

    fun createDeclarationCheckers(useExtendedCheckers: Boolean): DeclarationCheckers =
        if (useExtendedCheckers) extendedDeclarationCheckers else commonDeclarationCheckers

    fun createExpressionCheckers(useExtendedCheckers: Boolean): ExpressionCheckers =
        if (useExtendedCheckers) ExtendedExpressionCheckers else CommonExpressionCheckers

    // TODO hack to have all checkers present in DeclarationCheckers.memberDeclarationCheckers and similar
    // If use ExtendedDeclarationCheckers directly when DeclarationCheckers.memberDeclarationCheckers will not contain basicDeclarationCheckers
    @OptIn(SessionConfiguration::class)
    private fun createDeclarationCheckers(declarationCheckers: DeclarationCheckers): DeclarationCheckers =
        CheckersComponent().apply { register(declarationCheckers) }.declarationCheckers

}
