/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

object FirJvmModuleAccessibilityResolvedQualifierChecker : FirResolvedQualifierChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.symbol
        if (symbol is FirClassSymbol<*>) {
            FirJvmModuleAccessibilityQualifiedAccessChecker.checkClassAccess(context, symbol, expression, reporter)
        }
    }
}
