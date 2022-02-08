/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirVisibilityQualifierChecker : FirResolvedQualifierChecker() {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        checkClassLikeSymbol(expression.symbol ?: return, expression, context, reporter)
    }

    @OptIn(SymbolInternals::class)
    private fun checkClassLikeSymbol(
        symbol: FirClassLikeSymbol<*>,
        expression: FirResolvedQualifier,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val firFile = context.containingDeclarations.firstOrNull() as? FirFile ?: return
        val firClassLikeDeclaration = symbol.fir

        if (!context.session.visibilityChecker.isVisible(
                firClassLikeDeclaration, context.session, firFile, context.containingDeclarations,
                dispatchReceiver = null,
            )
        ) {
            reporter.reportOn(expression.source, FirErrors.INVISIBLE_REFERENCE, symbol, context)
            return
        }

        if (firClassLikeDeclaration is FirTypeAlias) {
            firClassLikeDeclaration.expandedConeType?.toSymbol(context.session)?.let {
                checkClassLikeSymbol(it, expression, context, reporter)
            }
        }

        symbol.getOwnerLookupTag()?.toSymbol(context.session)?.let {
            checkClassLikeSymbol(it, expression, context, reporter)
        }
    }
}
