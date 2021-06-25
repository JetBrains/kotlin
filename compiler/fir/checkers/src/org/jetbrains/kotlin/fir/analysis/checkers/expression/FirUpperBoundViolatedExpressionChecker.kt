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
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirUpperBoundViolatedExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleReference = expression.calleeReference
        var calleeFir: FirTypeParameterRefsOwner? = null
        if (calleReference is FirResolvedNamedReference) {
            calleeFir = calleReference.safeAs<FirResolvedNamedReference>()?.resolvedSymbol?.fir.safeAs()
        } else if (calleReference is FirErrorNamedReference) {
            val diagnostic = calleReference.diagnostic
            if (diagnostic is ConeInapplicableCandidateError &&
                diagnostic.applicability == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
            ) {
                return
            }
            calleeFir = calleReference.candidateSymbol?.fir.safeAs()
        }

        var typeArguments: List<Any>? = null
        var typeArgumentRefsAndSources: List<FirTypeRefSource?>? = null
        val typeParameters = if (calleeFir is FirConstructor) {
            typeArgumentRefsAndSources =
                expression.typeArguments.map { FirTypeRefSource((it as? FirTypeProjectionWithVariance)?.typeRef, it.source) }
            val prototypeClass = calleeFir.dispatchReceiverType?.toSymbol(context.session)?.fir.safeAs<FirRegularClass>()
            prototypeClass?.typeParameters?.map { it.symbol }
        } else {
            typeArguments = expression.typeArguments
            calleeFir?.typeParameters?.map { it.symbol }
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