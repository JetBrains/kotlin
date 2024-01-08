/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

object FirFunInterfaceConstructorReferenceChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirCallableReferenceAccess) return
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.KotlinFunInterfaceConstructorReference)) return

        val reference = expression.calleeReference.resolved ?: return
        val referredSymbol = reference.resolvedSymbol

        if (referredSymbol is FirNamedFunctionSymbol &&
            referredSymbol.origin == FirDeclarationOrigin.SamConstructor
        ) {
            val samClassSymbol = referredSymbol.resolvedReturnTypeRef.toRegularClassSymbol(context.session) ?: return
            if (samClassSymbol.isFun && !samClassSymbol.isJavaOrEnhancement) {
                reporter.reportOn(reference.source, FUN_INTERFACE_CONSTRUCTOR_REFERENCE, context)
            }
        }
    }
}
