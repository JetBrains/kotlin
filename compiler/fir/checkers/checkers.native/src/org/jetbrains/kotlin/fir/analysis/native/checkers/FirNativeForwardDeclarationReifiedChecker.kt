/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.type

object FirNativeForwardDeclarationReifiedChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeReference = expression.calleeReference
        val typeArguments = expression.typeArguments
        val typeParameters = calleeReference.toResolvedCallableSymbol()?.typeParameterSymbols ?: return

        val count = minOf(typeArguments.size, typeParameters.size)
        for (index in 0 until count) {
            val typeArgumentProjection = typeArguments.elementAt(index)
            val source = typeArgumentProjection.source ?: calleeReference.source ?: continue

            val typeArgument = typeArgumentProjection.toConeTypeProjection().type ?: continue
            val typeParameter = typeParameters[index]

            if (typeParameter.isReified && typeArgument.toRegularClassSymbol(context.session)?.forwardDeclarationKindOrNull() != null) {
                reporter.reportOn(
                    source,
                    FirNativeErrors.FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT,
                    typeArgument,
                    context,
                )
            }
        }
    }
}
