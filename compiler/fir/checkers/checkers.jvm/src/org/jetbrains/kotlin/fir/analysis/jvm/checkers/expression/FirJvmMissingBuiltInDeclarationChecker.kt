/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

object FirJvmMissingBuiltInDeclarationChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.getFlag(JvmAnalysisFlags.suppressMissingBuiltinsError)) return

        fun reportIfNeeded(classSymbol: FirClassLikeSymbol<*>?): Boolean {
            if (classSymbol?.origin == FirDeclarationOrigin.BuiltInsFallback) {
                reporter.reportOn(expression.source, FirJvmErrors.MISSING_BUILT_IN_DECLARATION, classSymbol, context)
                return true
            }
            return false
        }

        if (expression is FirResolvedQualifier) {
            if (reportIfNeeded(expression.symbol)) return
        }

        val resolvedReferenceSymbol = expression.toReference(context.session)?.resolved?.symbol
        val containingClass = resolvedReferenceSymbol?.getContainingClassSymbol(context.session)
        if (reportIfNeeded(containingClass)) return

        if (resolvedReferenceSymbol is FirCallableSymbol) {
            val returnTypeClassSymbol = resolvedReferenceSymbol.resolvedReturnTypeRef.toRegularClassSymbol(context.session)
            if (reportIfNeeded(returnTypeClassSymbol)) return
        }
    }
}