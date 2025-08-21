/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.sanitizeName
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.util.OperatorNameConventions

private val nameToOperator = mapOf(
    OperatorNameConventions.CONTAINS to "in",
    OperatorNameConventions.RANGE_TO to "..",
    OperatorNameConventions.RANGE_UNTIL to "..<",
)

object FirJsDynamicCallChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val callee = expression.calleeReference.resolved ?: return

        if (callee.resolvedSymbol.origin !is FirDeclarationOrigin.DynamicScope) {
            return checkSpreadOperator(expression)
        }

        val symbol = callee.toResolvedCallableSymbol()
            ?: error("Resolved call callee without a callable symbol")

        when {
            expression is FirCall && expression.isArrayAccessWithMultipleIndices(context.session) -> reporter.reportOn(
                expression.source, FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC, "indexed access with more than one index"
            )
            expression is FirFunctionCall && expression.isInOperator -> reporter.reportOn(
                expression.source, FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC, "`in` operation"
            )
            expression is FirFunctionCall && expression.isRangeOperator -> reporter.reportOn(
                expression.source, FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC, "`${nameToOperator[symbol.name]}` operation"
            )
            expression is FirComponentCall -> reporter.reportOn(
                expression.source, FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC, "`destructuring declaration"
            )
            else -> checkIdentifier(callee)
        }

        forAllSpreadArgumentsOf(expression) {
            reporter.reportOn(it.source, FirJsErrors.SPREAD_OPERATOR_IN_DYNAMIC_CALL)
        }
    }

    private fun FirCall.isArrayAccessWithMultipleIndices(session: FirSession): Boolean {
        val callee = toReference(session) as? FirNamedReference
            ?: return false

        if (callee.source?.kind != KtFakeSourceElementKind.ArrayAccessNameReference) {
            return false
        }

        val arguments = (arguments.singleOrNull() as? FirVarargArgumentsExpression)?.arguments
            ?: return false

        return callee.name == OperatorNameConventions.GET && arguments.size >= 2
                || callee.name == OperatorNameConventions.SET && arguments.size >= 3
    }

    private val FirFunctionCall.isInOperator
        get() = calleeReference.resolved?.name == OperatorNameConventions.CONTAINS && origin == FirFunctionCallOrigin.Operator

    private val FirFunctionCall.isRangeOperator
        get(): Boolean {
            val name = calleeReference.resolved?.name
            return (name == OperatorNameConventions.RANGE_TO || name == OperatorNameConventions.RANGE_UNTIL)
                    && origin == FirFunctionCallOrigin.Operator
        }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpreadOperator(
        expression: FirQualifiedAccessExpression,
    ) {
        forAllSpreadArgumentsOf(expression) {
            if (it.resolvedType is ConeDynamicType) {
                reporter.reportOn(it.source, FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC, "spread operator")
            }
        }
    }

    private inline fun forAllSpreadArgumentsOf(expression: FirQualifiedAccessExpression, callback: (FirExpression) -> Unit) {
        val call = expression as? FirCall ?: return
        for (argument in call.argumentList.arguments) {
            if (argument !is FirVarargArgumentsExpression) {
                continue
            }

            for (it in argument.arguments) {
                if (it is FirSpreadArgumentExpression) {
                    callback(it)
                }
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkIdentifier(
        namedReference: FirResolvedNamedReference,
    ) {
        if (LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping.isEnabled()) {
            return
        }
        val name = namedReference.name.identifierOrNullIfSpecial ?: return
        if (sanitizeName(name) != name) {
            reporter.reportOn(namedReference.source, FirJsErrors.NAME_CONTAINS_ILLEGAL_CHARS)
        }
    }
}
