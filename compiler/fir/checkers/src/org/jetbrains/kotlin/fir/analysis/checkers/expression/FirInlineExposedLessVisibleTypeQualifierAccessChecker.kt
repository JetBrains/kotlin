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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.forEachType

object FirInlineExposedLessVisibleTypeQualifierAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Platform) {
    context(c: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val inlineFunctionBodyContext = c.inlineFunctionBodyContext ?: return

        // We don't care about public functions because other diagnostics are already reported on them
        if (inlineFunctionBodyContext.inlineFunEffectiveVisibility == EffectiveVisibility.Public) return

        if (c.callsOrAssignments.any { it is FirAnnotation }) return

        val symbol = expression.toResolvedCallableSymbol() ?: return
        if (symbol.effectiveVisibility is EffectiveVisibility.Local) return

        fun ConeKotlinType.reportIfLessVisible() {
            fullyExpandedType(inlineFunctionBodyContext.session).forEachType { type ->
                val symbolEffectiveVisibility =
                    type.toClassLikeSymbol(inlineFunctionBodyContext.session)
                        ?.let { it.publishedApiEffectiveVisibility ?: it.effectiveVisibility } ?: return@forEachType

                if (inlineFunctionBodyContext.isLessVisibleThanInlineFunction(symbolEffectiveVisibility)) {
                    reporter.reportOn(
                        expression.source, FirErrors.LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE,
                        symbol,
                        symbolEffectiveVisibility,
                        type,
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

        fun FirRegularClassSymbol.reportIfLessVisible() {
            val containingClassLookupTag = getContainingClassLookupTag()
            val effectiveVisibility = visibility.toEffectiveVisibility(containingClassLookupTag, true)
            if (inlineFunctionBodyContext.isLessVisibleThanInlineFunction(effectiveVisibility) && publishedApiEffectiveVisibility == null) {
                reporter.reportOn(
                    expression.source,
                    FirErrors.LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE,
                    symbol,
                    effectiveVisibility,
                    this,
                    inlineFunctionBodyContext.inlineFunEffectiveVisibility
                )
                // Stop recursion to prevent multiple errors
                return
            }
            containingClassLookupTag?.toRegularClassSymbol(c.session)?.reportIfLessVisible()
        }

        // We don't check the visibility of the declaration itself because we generate synthetic bridges if necessary
        // and it won't lead to runtime crashes.
        symbol.containingClassLookupTag()?.toRegularClassSymbol(c.session)?.reportIfLessVisible()
    }
}