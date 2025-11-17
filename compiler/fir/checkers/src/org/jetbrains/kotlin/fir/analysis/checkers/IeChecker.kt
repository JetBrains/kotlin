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
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.equalTypes
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
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
    val isReified: Boolean? = null,
    val oneLevelRedundant: Boolean? = null,
    val oneLevelRedundantIncludingLambdas: Boolean? = null,
    val upperBound: Boolean? = null,
    val upperBoundedByTp: Boolean? = null,
)

object FirMyChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)
        val symbol = expression.toResolvedCallableSymbol() ?: return
        val usedParameters = buildSet {
            symbol.fir.returnTypeRef.coneType.forEachType { type ->
                if (type is ConeTypeParameterType) {
                    add(type.lookupTag.typeParameterSymbol)
                }
            }
        }
        val usedInLambdas = buildSet {
            (symbol.fir as? FirFunction)?.let { function ->
                function.valueParameters.lastOrNull()?.let { lastParam ->
                    if (lastParam.returnTypeRef.coneType.isSomeFunctionType(context.session)) {
                        lastParam.returnTypeRef.coneType.forEachType { type ->
                            if (type is ConeTypeParameterType) {
                                add(type.lookupTag.typeParameterSymbol)
                            }
                        }
                    }
                }
            }
        }
        symbol.typeParameterSymbols.zip(expression.typeArguments).forEach { (param, type) ->
            if (!type.isExplicit) return@forEach
            val bound = if (param.resolvedBounds.size == 1) param.resolvedBounds.first().coneType else null
            if (type !is FirTypeProjectionWithVariance) return@forEach
            val upperBound = bound?.equalTypes(type.typeRef.coneType, context.session) ?: false
            val upperBoundedByTp = bound is ConeTypeParameterType
            val oneLevelRedundant = param !in usedParameters
            val oneLevelRedundantIncludingLambdas = param !in usedInLambdas && param !in usedParameters
            report(
                IEData(
                    type = type.render(),
                    call = symbol.name.toString(),
                    isReified = param.isReified,
                    oneLevelRedundant = oneLevelRedundant,
                    oneLevelRedundantIncludingLambdas = oneLevelRedundantIncludingLambdas,
                    upperBound = upperBound,
                    upperBoundedByTp = upperBoundedByTp,
                )
            )
        }
    }
}
