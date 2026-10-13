/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkMissingDependencySuperTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirFunctionConversionKind
import org.jetbrains.kotlin.fir.expressions.FirFunctionTypeConversionExpression
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * @see FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker for SAM constructors (KT-81075).
 */
object FirMissingDependencySupertypeInSamConversionChecker : FirFunctionTypeConversionExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionTypeConversionExpression) {
        if (expression.kind != FirFunctionConversionKind.Sam) return

        val samClassSymbol = expression.resolvedType.toRegularClassSymbol()
        checkMissingDependencySuperTypes(samClassSymbol, expression.source, ForbidSamConstructorCallsWithMissingDependencySupertype)
    }
}
