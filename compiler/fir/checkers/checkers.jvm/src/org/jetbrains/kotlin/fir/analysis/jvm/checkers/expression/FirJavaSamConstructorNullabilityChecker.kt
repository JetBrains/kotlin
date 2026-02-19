/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getReturnedExpressions
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirJavaSamConstructorNullabilityChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val reportError =
            LanguageFeature.ProhibitReturningIncorrectNullabilityValuesFromSamConstructorLambdaOfJdkInterfaces.isEnabled()

        val calleeReference = expression.calleeReference
        if (calleeReference.isError()) return
        val symbol = calleeReference.toResolvedFunctionSymbol() ?: return
        if (symbol.origin != FirDeclarationOrigin.SamConstructor) return
        if (symbol.resolvedReturnType.toRegularClassSymbol()?.isJavaOrEnhancement != true) return

        val (lambda, parameter) = expression.resolvedArgumentMapping?.entries?.singleOrNull() ?: return
        if (lambda !is FirAnonymousFunctionExpression) return

        val parameterFunctionType = parameter.returnTypeRef.coneType
        val substitutor = expression.createConeSubstitutorFromTypeArguments(
            symbol, context.session,
            unwrapExplicitTypeArgumentForMadeFlexibleSynthetically = true,
        )
        val expectedReturnType = parameterFunctionType.typeArguments.lastOrNull()?.type?.let(substitutor::substituteOrSelf) ?: return

        for (returnedExpression in lambda.anonymousFunction.symbol.getReturnedExpressions()) {
            val returnedExpressionType = returnedExpression.resolvedType
            if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, returnedExpressionType, expectedReturnType)) {
                if (reportError) {
                    reporter.reportOn(
                        returnedExpression.source,
                        FirErrors.ARGUMENT_TYPE_MISMATCH,
                        returnedExpressionType,
                        expectedReturnType,
                        true
                    )
                } else {
                    reporter.reportOn(
                        returnedExpression.source,
                        FirJvmErrors.TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES,
                        returnedExpressionType,
                        expectedReturnType
                    )
                }
            }
        }
    }
}
