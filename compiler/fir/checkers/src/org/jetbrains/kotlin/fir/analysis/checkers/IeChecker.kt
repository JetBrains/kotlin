/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.equalTypes
import org.jetbrains.kotlin.fir.types.forEachType
import kotlin.reflect.full.memberProperties

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
    val type: String? = null,
    val call: String? = null,
)

object FirMyChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)
        val symbol = expression.toResolvedCallableSymbol() ?: return
        symbol.typeParameterSymbols.zip(expression.typeArguments).forEach { (param, type) ->
            if (!type.isExplicit) return@forEach
            if (param.resolvedBounds.size != 1) return@forEach
            val bound = param.resolvedBounds.first()
            type as? FirTypeProjectionWithVariance ?: return@forEach
            if (!bound.coneType.equalTypes(type.typeRef.coneType, context.session)) return@forEach
            report(
                IEData(
                    type = type.render(),
                    call = symbol.name.toString()
                )
            )
        }
    }
}
