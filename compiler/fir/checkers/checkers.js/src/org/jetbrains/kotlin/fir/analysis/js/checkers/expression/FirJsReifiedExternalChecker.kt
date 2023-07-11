/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeInterface
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.toSymbol

object FirJsReifiedExternalChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val functionSymbol = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        for ((typeParameterSymbol, typeArgument) in functionSymbol.typeParameterSymbols.zip(expression.typeArguments)) {
            if (typeParameterSymbol.isReified) {
                val type = (typeArgument as? FirTypeProjectionWithVariance)?.typeRef?.coneTypeOrNull ?: continue
                val typeSymbol = type.toSymbol(context.session) ?: continue
                if (typeSymbol.isNativeInterface(context)) {
                    reporter.reportOn(
                        typeArgument.source ?: expression.source,
                        FirJsErrors.EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT,
                        type,
                        context
                    )
                }
            }
        }
    }

}
