/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isUnit

object FirStandaloneQualifierChecker : FirResolvedQualifierChecker() {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val lastQualifiedAccess = context.qualifiedAccessOrAnnotationCalls.lastOrNull() as? FirQualifiedAccess
        if (lastQualifiedAccess?.explicitReceiver === expression) return
        val lastGetClass = context.getClassCalls.lastOrNull()
        if (lastGetClass?.argument === expression) return
        // Note: if it's real Unit, it will be filtered by ClassKind.OBJECT check below
        if (!expression.typeRef.isUnit) return

        when (val symbol = expression.symbol) {
            is FirRegularClassSymbol -> {
                if (symbol.classKind == ClassKind.OBJECT) return
                reporter.reportOn(expression.source, FirErrors.NO_COMPANION_OBJECT, symbol, context)
            }
            null -> {
                reporter.reportOn(expression.source, FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND, context)
            }
            else -> {}
        }
    }
}
