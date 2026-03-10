/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.isEffectivelyLocal
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import kotlin.reflect.full.memberProperties

object FirSmartCastRelyingOnCallsInPlaceChecker : FirSmartCastExpressionChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirSmartCastExpression) {
        if (!expression.isStable) return

        val source = expression.source ?: return

        val originalExpression = expression.originalExpression as? FirQualifiedAccessExpression ?: return
        val propertySymbol = originalExpression.calleeReference.toResolvedSymbol<FirPropertySymbol>() ?: return
        val property = propertySymbol.fir

        if (!property.isEffectivelyLocal) return

        val containingLambda =
            context.containingElements.asReversed().firstOrNull { it is FirAnonymousFunction } as? FirAnonymousFunction ?: return
        val callsInPlaceType = containingLambda.invocationKind
        if (callsInPlaceType == null || !callsInPlaceType.isInPlace) return
        val report = IEReporter(source, context, reporter, FirErrors.SMARTCAST_RELYING_ON_CALLS_IN_PLACE)
        report(
            IEData(
                info = "",
                variableName = property.name.asString(),
                resolvedType = expression.resolvedType.toString(),
                callsInPlaceType = callsInPlaceType.toString(),
                isInline = (containingLambda.inlineStatus == InlineStatus.Inline).toString()
            )
        )
    }
}


class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: IEData) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IEData(
    val info: String? = null,
    val variableName: String? = null,
    val resolvedType: String? = null,
    val callsInPlaceType: String? = null,
    val isInline: String? = null,
)
