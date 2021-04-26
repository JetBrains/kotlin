/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.checkers.*

internal abstract class AbstractFirIdeDiagnosticsCollector(
    session: FirSession,
    useExtendedCheckers: Boolean,
) : AbstractDiagnosticCollector(
    session,
    createComponents = { reporter ->
        CheckersFactory.createComponents(session, reporter, useExtendedCheckers)
    }
)


private object CheckersFactory {
    fun createComponents(
        session: FirSession,
        reporter: DiagnosticReporter,
        useExtendedCheckers: Boolean
    ): List<AbstractDiagnosticCollectorComponent> {
        val declarationCheckers = createDeclarationCheckers(useExtendedCheckers)
        val expressionCheckers = createExpressionCheckers(useExtendedCheckers)
        val typeCheckers = createTypeCheckers(useExtendedCheckers)

        return if (useExtendedCheckers) {
            listOf(
                DeclarationCheckersDiagnosticComponent(session, reporter, declarationCheckers),
                ExpressionCheckersDiagnosticComponent(session, reporter, expressionCheckers),
                TypeCheckersDiagnosticComponent(session, reporter, typeCheckers),
                ControlFlowAnalysisDiagnosticComponent(session, reporter, declarationCheckers),
            )
        } else {
            listOf(
                DeclarationCheckersDiagnosticComponent(session, reporter, declarationCheckers),
                ExpressionCheckersDiagnosticComponent(session, reporter, expressionCheckers),
                TypeCheckersDiagnosticComponent(session, reporter, typeCheckers),
                ErrorNodeDiagnosticCollectorComponent(session, reporter),
                ControlFlowAnalysisDiagnosticComponent(session, reporter, declarationCheckers),
            )
        }
    }

    private val extendedDeclarationCheckers = createDeclarationCheckers(ExtendedDeclarationCheckers)

    private val commonDeclarationCheckers = createDeclarationCheckers(CommonDeclarationCheckers,)

    private fun createDeclarationCheckers(useExtendedCheckers: Boolean): DeclarationCheckers =
        if (useExtendedCheckers) extendedDeclarationCheckers else commonDeclarationCheckers

    private fun createExpressionCheckers(useExtendedCheckers: Boolean): ExpressionCheckers =
        if (useExtendedCheckers) ExtendedExpressionCheckers else CommonExpressionCheckers

    private fun createTypeCheckers(useExtendedCheckers: Boolean): TypeCheckers = CommonTypeCheckers

    // TODO hack to have all checkers present in DeclarationCheckers.memberDeclarationCheckers and similar
    // If use ExtendedDeclarationCheckers directly when DeclarationCheckers.memberDeclarationCheckers will not contain basicDeclarationCheckers
    @OptIn(SessionConfiguration::class)
    private fun createDeclarationCheckers(vararg declarationCheckers: DeclarationCheckers): DeclarationCheckers =
        CheckersComponent().apply { declarationCheckers.forEach(::register) }.declarationCheckers

}
