/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.fir.types.hasContextParameters

object FirContextParameterInCalledSignatureChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (LanguageFeature.ContextParameters.isEnabled() ||
            LanguageFeature.ContextReceivers.isEnabled()
        ) {
            return
        }

        if (expression.toResolvedCallableSymbol()?.hasContextualFunctionTypeInSignature() == true) {
            reporter.reportOn(expression.source, FirErrors.UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL)
        }
    }


    context(context: CheckerContext)
    private fun FirCallableSymbol<*>.hasContextualFunctionTypeInSignature(): Boolean {
        if (resolvedReceiverType?.hasContextParametersFullyExpanded() == true) return true

        // No need for checking context parameters, if there are any, we will report UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL anyway.

        if (resolvedReturnType.hasContextParametersFullyExpanded()) return true
        if (typeParameterSymbols.any { it.resolvedBounds.any { it.coneType.hasContextParametersFullyExpanded() } }) return true

        val functionSymbol = this as? FirFunctionSymbol ?: return false
        return functionSymbol.valueParameterSymbols.any { it.resolvedReturnType.hasContextParametersFullyExpanded() }
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.hasContextParametersFullyExpanded(): Boolean {
        return fullyExpandedType().contains {
            it.hasContextParameters
        }
    }
}