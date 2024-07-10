/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeNotFunctionAsOperator
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object FirConventionFunctionCallChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        checkNotFunctionAsOperator(expression, expression.dispatchReceiver, context, reporter)
        checkNotFunctionAsOperator(expression, expression.extensionReceiver, context, reporter)
        val calleeReference = expression.calleeReference as? FirErrorNamedReference ?: return
        val diagnostic = calleeReference.diagnostic as? ConeUnresolvedNameError ?: return

        if (expression.calleeReference.source?.kind == KtFakeSourceElementKind.ArrayAccessNameReference) {
            when (diagnostic.name) {
                OperatorNameConventions.GET -> reporter.reportOn(calleeReference.source, FirErrors.NO_GET_METHOD, context)
                OperatorNameConventions.SET -> reporter.reportOn(calleeReference.source, FirErrors.NO_SET_METHOD, context)
            }
        }
    }

    private fun checkNotFunctionAsOperator(
        callExpression: FirFunctionCall,
        receiver: FirExpression?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (callExpression.dispatchReceiver?.resolvedType is ConeDynamicType) return
        // KT-61905: TODO: Return also in case of error type.
        val unwrapped = receiver?.unwrapSmartcastExpression() ?: return
        val nonFatalDiagnostics = when (unwrapped) {
            is FirQualifiedAccessExpression -> unwrapped.nonFatalDiagnostics
            is FirResolvedQualifier -> unwrapped.nonFatalDiagnostics
            else -> return
        }
        val diagnosticSymbol = nonFatalDiagnostics.firstIsInstanceOrNull<ConeNotFunctionAsOperator>()?.symbol ?: return
        when {
            unwrapped.resolvedType.classId!!.shortClassName == OperatorNameConventions.ITERATOR -> {
                reporter.reportOn(unwrapped.source, FirErrors.ITERATOR_MISSING, context)
            }
            else -> {
                // NOT_FUNCTION_AS_OPERATOR can only happen for function calls and it's reported on the receiver expression.
                reporter.reportOn(
                    callExpression.calleeReference.source,
                    FirErrors.NOT_FUNCTION_AS_OPERATOR,
                    if (diagnosticSymbol is FirPropertySymbol) "Property" else "Object",
                    diagnosticSymbol,
                    context
                )
            }
        }
    }
}
