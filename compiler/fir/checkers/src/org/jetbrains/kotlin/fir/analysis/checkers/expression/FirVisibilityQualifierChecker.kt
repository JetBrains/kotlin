/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.firForVisibilityChecker
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirVisibilityQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        checkClassLikeSymbol(expression.symbol ?: return, expression, expression.isStandalone(context), context, reporter)
    }

    private fun checkClassLikeSymbol(
        symbol: FirClassLikeSymbol<*>,
        expression: FirResolvedQualifier,
        isStandalone: Boolean,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val firFile = context.containingFile ?: return

        // Note: errors on implicit receiver are already reported in coneDiagnosticToFirDiagnostic
        // See e.g. diagnostics/tests/visibility/packagePrivateStaticViaInternal.fir.kt
        if (expression.source?.kind != KtFakeSourceElementKind.ImplicitReceiver &&
            !context.session.visibilityChecker.isClassLikeVisible(
                symbol.firForVisibilityChecker,
                context.session, firFile, context.containingDeclarations,
            )
        ) {
            if (expression !is FirErrorResolvedQualifier || expression.diagnostic !is ConeVisibilityError) {
                @OptIn(DirectDeclarationsAccess::class)
                if (context.containingFile?.declarations?.singleOrNull() !is FirCodeFragment) {
                    reporter.report(symbol.toInvisibleReferenceDiagnostic(expression.source, context.session), context)
                }
            }

            return
        }

        // Validate standalone references to companion objects are visible. Qualified use is validated
        // by call resolution cone diagnostics in coneDiagnosticToFirDiagnostic.
        if (isStandalone) {
            val invisibleCompanion = expression.symbol?.fullyExpandedClass(context.session)?.toInvisibleCompanion(context)
            if (invisibleCompanion != null) {
                if (expression !is FirErrorResolvedQualifier || expression.diagnostic !is ConeVisibilityError) {
                    @OptIn(DirectDeclarationsAccess::class)
                    if (context.containingFile?.declarations?.singleOrNull() !is FirCodeFragment) {
                        reporter.report(invisibleCompanion.toInvisibleReferenceDiagnostic(expression.source, context.session), context)
                    }
                }

                return
            }
        }

        if (symbol is FirTypeAliasSymbol) {
            symbol.resolvedExpandedTypeRef.coneType.toClassLikeSymbol(context.session)?.let {
                checkClassLikeSymbol(it, expression, isStandalone, context, reporter)
            }
        }

        symbol.getOwnerLookupTag()?.toSymbol(context.session)?.let {
            checkClassLikeSymbol(it, expression, isStandalone = false, context, reporter)
        }
    }

    private fun FirRegularClassSymbol.toInvisibleCompanion(context: CheckerContext): FirRegularClassSymbol? {
        val firFile = context.containingFile ?: return null
        return companionObjectSymbol?.takeIf {
            !context.session.visibilityChecker.isClassLikeVisible(
                it.firForVisibilityChecker, context.session, firFile, context.containingDeclarations,
            )
        }
    }
}
