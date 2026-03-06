/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicit
import org.jetbrains.kotlin.fir.analysis.checkers.isStandalone
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType

object FirStandaloneQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        if (!expression.isStandalone()) return

        // Note: if it's real Unit, it will be filtered by ClassKind.OBJECT check below in reportErrorOn
        if (!expression.resolvedType.isUnit) {
            if (expression.typeArguments.any { it.isExplicit }) {
                reporter.reportOn(expression.source, FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS, "Object")
            }
            return
        }

        expression.symbol.reportErrorOn(expression.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirBasedSymbol<*>?.reportErrorOn(
        source: KtSourceElement?,
    ) {
        when (this) {
            is FirRegularClassSymbol -> {
                if (classKind == ClassKind.OBJECT) return
                reporter.reportOn(source, FirErrors.NO_COMPANION_OBJECT, this)
            }
            is FirTypeAliasSymbol -> {
                fullyExpandedClass()?.reportErrorOn(source)
            }
            null -> {
                reporter.reportOn(source, FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND)
            }
            else -> {}
        }
    }
}
