/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.fir.types.forEachType

object FirInlineExposedLessVisibleTypeQualifierAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Platform) {
    context(c: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val inlineFunctionBodyContext = c.inlineFunctionBodyContext ?: return

        // We don't care about public functions because other diagnostics are already reported on them
        if (inlineFunctionBodyContext.inlineFunEffectiveVisibility == EffectiveVisibility.Public) return

        val symbol = expression.toResolvedCallableSymbol() ?: return
        if (symbol.isLocalMember) return

        fun ConeKotlinType.reportIfLessVisible() {
            fullyExpandedType(inlineFunctionBodyContext.session).forEachType {
                val symbolEffectiveVisibility =
                    it.toClassLikeSymbol(inlineFunctionBodyContext.session)?.effectiveVisibility ?: return@forEachType

                if (inlineFunctionBodyContext.isLessVisibleThanInlineFunction(symbolEffectiveVisibility)) {
                    reporter.reportOn(
                        expression.source, FirErrors.LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE,
                        symbol,
                        symbolEffectiveVisibility,
                        it,
                        inlineFunctionBodyContext.inlineFunEffectiveVisibility,
                    )
                }
            }
        }

        symbol.contextParameterSymbols.forEach { it.resolvedReturnType.reportIfLessVisible() }
        symbol.receiverParameterSymbol?.resolvedType?.reportIfLessVisible()
        if (symbol is FirFunctionSymbol) {
            symbol.valueParameterSymbols.forEach { it.resolvedReturnType.reportIfLessVisible() }
        }
        symbol.typeParameterSymbols.forEach { typeParameterSymbol ->
            typeParameterSymbol.resolvedBounds.forEach { it.coneType.reportIfLessVisible() }
        }
        symbol.resolvedReturnType.reportIfLessVisible()
    }
}