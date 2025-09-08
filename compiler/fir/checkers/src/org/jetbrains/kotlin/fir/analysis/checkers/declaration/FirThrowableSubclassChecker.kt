/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeOfThrowable
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType

object FirThrowableSubclassChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (!declaration.hasThrowableSupertype())
            return

        if (declaration.typeParameters.isNotEmpty()) {
            declaration.typeParameters.firstOrNull()?.source?.let {
                reporter.reportOn(it, FirErrors.GENERIC_THROWABLE_SUBCLASS)
            }

            val shouldReport = when (declaration) {
                is FirRegularClass -> declaration.isInner || declaration.isLocal
                is FirAnonymousObject -> true
            }

            if (shouldReport) {
                reporter.reportOn(declaration.source, FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS)
            }
        } else if (declaration.hasGenericOuterDeclaration()) {
            reporter.reportOn(declaration.source, FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS)
        }
    }

    context(context: CheckerContext)
    private fun FirClass.hasThrowableSupertype() =
        superConeTypes.any { it !is ConeErrorType && it.isSubtypeOfThrowable(context.session) }

    context(context: CheckerContext)
    private fun FirClass.hasGenericOuterDeclaration(): Boolean {
        if (!isLocal) return false
        for (containingDeclaration in context.containingDeclarations.asReversed()) {
            val hasTypeParameters = when (containingDeclaration) {
                is FirCallableSymbol -> containingDeclaration.typeParameterSymbols.isNotEmpty()
                is FirClassLikeSymbol -> containingDeclaration.typeParameterSymbols.isNotEmpty()
                else -> false
            }
            if (hasTypeParameters) {
                return true
            }
            if (containingDeclaration is FirRegularClassSymbol && !containingDeclaration.isLocal && !containingDeclaration.isInner) {
                return false
            }
        }
        return false
    }
}
