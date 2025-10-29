/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.resolvedSymbolOrCompanionSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirInlineExposedLessVisibleTypeQualifiedAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return

        // We don't care about public functions because other diagnostics are already reported on them
        if (inlineFunctionBodyContext.inlineFunEffectiveVisibility == EffectiveVisibility.Public) return

        if (context.callsOrAssignments.any { it is FirAnnotation }) return

        val symbol = expression.toResolvedCallableSymbol() ?: return
        if (symbol.effectiveVisibility is EffectiveVisibility.Local) return

        // Calls to local callables are allowed to contain local/anonymous types in their signatures
        fun ConeKotlinType.reportIfLessVisible(ignoreLocal: Boolean) {
            fullyExpandedType().forEachType { type ->
                val classLikeSymbol = type.toClassLikeSymbol() ?: return@forEachType

                val symbolEffectiveVisibility = inlineFunctionBodyContext
                    .lessVisibleVisibilityOrNull(classLikeSymbol, ignoreLocal)
                    ?: return@forEachType

                reporter.reportOn(
                    expression.source, FirErrors.LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE,
                    symbol,
                    symbolEffectiveVisibility,
                    type,
                    inlineFunctionBodyContext.inlineFunEffectiveVisibility,
                )
            }
        }

        symbol.contextParameterSymbols.forEach { it.resolvedReturnType.reportIfLessVisible(ignoreLocal = false) }
        symbol.receiverParameterSymbol?.resolvedType?.reportIfLessVisible(ignoreLocal = false)
        if (symbol is FirFunctionSymbol) {
            symbol.valueParameterSymbols.forEach { it.resolvedReturnType.reportIfLessVisible(ignoreLocal = false) }
        }
        symbol.typeParameterSymbols.forEach { typeParameterSymbol ->
            typeParameterSymbol.resolvedBounds.forEach { it.coneType.reportIfLessVisible(ignoreLocal = false) }
        }
        symbol.resolvedReturnType.reportIfLessVisible(ignoreLocal = false)

        expression.dispatchReceiver?.let {
            if (it is FirResolvedQualifier) {
                it.resolvedSymbolOrCompanionSymbol()
                    ?.defaultType()
                    ?.reportIfLessVisible(ignoreLocal = true)
            } else {
                it.resolvedType.reportIfLessVisible(ignoreLocal = true)
            }
        }
    }
}