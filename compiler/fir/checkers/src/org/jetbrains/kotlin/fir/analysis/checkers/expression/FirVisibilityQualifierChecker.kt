/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isStandalone
import org.jetbrains.kotlin.fir.analysis.diagnostics.toInvisibleReferenceDiagnostic
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.isClassLikeVisible
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirVisibilityQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        checkClassLikeSymbol(expression.symbol ?: return, expression, expression.isStandalone())
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkClassLikeSymbol(
        symbol: FirClassLikeSymbol<*>,
        expression: FirResolvedQualifier,
        isStandalone: Boolean,
    ) {
        val containingFileSymbol = context.containingFileSymbol ?: return

        // Note: errors on implicit receiver are already reported in coneDiagnosticToFirDiagnostic
        // See e.g. diagnostics/tests/visibility/packagePrivateStaticViaInternal.fir.kt
        if (expression.source?.kind != KtFakeSourceElementKind.ImplicitReceiver &&
            !context.session.visibilityChecker.isClassLikeVisible(
                symbol,
                context.session,
                containingFileSymbol,
                context.containingDeclarations,
            )
        ) {
            if (expression !is FirErrorResolvedQualifier || expression.diagnostic !is ConeVisibilityError) {
                @OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
                if (context.containingFileSymbol?.fir?.declarations?.singleOrNull() !is FirCodeFragment) {
                    reporter.report(symbol.toInvisibleReferenceDiagnostic(expression.source, context.session), context)
                }
            }

            return
        }

        // Validate standalone references to companion objects are visible. Qualified use is validated
        // by call resolution cone diagnostics in coneDiagnosticToFirDiagnostic.
        if (isStandalone) {
            val invisibleCompanion = expression.symbol?.fullyExpandedClass()?.toInvisibleCompanion()
            if (invisibleCompanion != null) {
                if (expression !is FirErrorResolvedQualifier || expression.diagnostic !is ConeVisibilityError) {
                    @OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
                    if (context.containingFileSymbol?.fir?.declarations?.singleOrNull() !is FirCodeFragment) {
                        reporter.report(invisibleCompanion.toInvisibleReferenceDiagnostic(expression.source, context.session), context)
                    }
                }

                return
            }
        }

        if (symbol is FirTypeAliasSymbol) {
            symbol.resolvedExpandedTypeRef.coneType.toClassLikeSymbol()?.let {
                checkClassLikeSymbol(it, expression, isStandalone)
            }
        }

        symbol.getOwnerLookupTag()?.toSymbol()?.let {
            checkClassLikeSymbol(it, expression, isStandalone = false)
        }
    }

    context(context: CheckerContext)
    private fun FirRegularClassSymbol.toInvisibleCompanion(): FirRegularClassSymbol? {
        val firFile = context.containingFileSymbol ?: return null
        return resolvedCompanionObjectSymbol?.takeIf {
            !context.session.visibilityChecker.isClassLikeVisible(
                it, context.session, firFile, context.containingDeclarations,
            )
        }
    }
}
