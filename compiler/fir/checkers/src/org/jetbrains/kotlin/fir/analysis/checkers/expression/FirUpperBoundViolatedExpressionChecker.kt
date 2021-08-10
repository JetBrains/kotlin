/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.FirTypeRefSource
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

object FirUpperBoundViolatedExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleReference = expression.calleeReference
        var calleeSymbol: FirCallableSymbol<*>? = null
        if (calleReference is FirResolvedNamedReference) {
            calleeSymbol = calleReference.toResolvedCallableSymbol()
        } else if (calleReference is FirErrorNamedReference) {
            if (calleReference.diagnostic is ConeInapplicableWrongReceiver) {
                return
            }
            calleeSymbol = calleReference.candidateSymbol as? FirCallableSymbol<*>
        }

        var typeArguments: List<Any>? = null
        var typeArgumentRefsAndSources: List<FirTypeRefSource?>? = null
        val typeParameters = if (calleeSymbol is FirConstructorSymbol) {
            typeArgumentRefsAndSources =
                expression.typeArguments.map { FirTypeRefSource((it as? FirTypeProjectionWithVariance)?.typeRef, it.source) }
            val prototypeClass = calleeSymbol.dispatchReceiverType?.toSymbol(context.session) as? FirRegularClassSymbol
            prototypeClass?.typeParameterSymbols
        } else {
            typeArguments = expression.typeArguments
            calleeSymbol?.typeParameterSymbols
        }

        checkUpperBoundViolated(
            expression.typeRef,
            context,
            reporter,
            typeParameters,
            typeArguments,
            typeArgumentRefsAndSources
        )
    }
}
