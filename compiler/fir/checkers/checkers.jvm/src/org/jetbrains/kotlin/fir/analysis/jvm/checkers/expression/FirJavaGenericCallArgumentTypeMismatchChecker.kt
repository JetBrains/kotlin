/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.createConeSubstitutorFromTypeArguments
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.resolvedType

object FirJavaGenericCallArgumentTypeMismatchChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.typeArguments.any { it.source?.kind == KtRealSourceElementKind }) {
            return
        }
        val reference = expression.calleeReference
        if (reference.isError()) return
        val symbol = reference.toResolvedFunctionSymbol() ?: return
        val substitutor = expression.createConeSubstitutorFromTypeArguments(
            symbol, context.session,
            unwrapExplicitTypeArgumentForMadeFlexibleSynthetically = false,
        )

        val argumentList = expression.argumentList as? FirResolvedArgumentList ?: return
        for ([argument, parameter] in argumentList.mapping) {
            val parameterType = parameter.returnTypeRef.coneType.let(substitutor::substituteOrSelf)
            if (parameterType.toRegularClassSymbol()?.isJavaOrEnhancement != true) continue
            val argumentType = argument.resolvedType
            if (!argumentType.isSubtypeOf(parameterType, context.session)) {
                reporter.reportOn(
                    argument.source,
                    FirErrors.ARGUMENT_TYPE_MISMATCH,
                    argumentType,
                    parameterType,
                    true
                )
            }
        }
    }
}
