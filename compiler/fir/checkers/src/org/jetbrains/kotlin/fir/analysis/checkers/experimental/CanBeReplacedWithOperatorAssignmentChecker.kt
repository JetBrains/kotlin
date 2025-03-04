/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.experimental

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.builder.toBinaryName
import org.jetbrains.kotlin.fir.dispatchReceiverClassTypeOrNull
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.util.OperatorNameConventions.BINARY_OPERATION_NAMES
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.TIMES

val PRECEDENCE_MAP: Map<Name, KotlinExpressionParsing.Precedence> = KotlinExpressionParsing.Precedence.entries
    .flatMap { precedence -> precedence.operations.types.map { it.toBinaryName()?.to(precedence) } }
    .filterNotNull()
    .toMap()

object CanBeReplacedWithOperatorAssignmentChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val lValue = expression.calleeReference
        if (lValue !is FirResolvedNamedReference) return
        if (expression.source?.kind is KtFakeSourceElementKind) return
        if (!expression.lValue.isPropertyAccessWithNoReceiver) return

        val rValue = expression.rValue as? FirFunctionCall ?: return
        if (rValue.source?.kind is KtFakeSourceElementKind) return

        if (rValue.explicitReceiver?.resolvedType?.fullyExpandedType(context.session)?.isPrimitive != true) return
        val rValueResolvedSymbol = rValue.toResolvedCallableSymbol() ?: return
        if (rValueResolvedSymbol.dispatchReceiverClassTypeOrNull()?.isPrimitive != true) return

        if (canBeReplaced(lValue, rValue, context)) {
            reporter.reportOn(expression.source, FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT, context)
        }
    }

    private val FirExpression.isPropertyAccessWithNoReceiver
        get() = this is FirPropertyAccessExpression && explicitReceiver.let {
            it == null || it is FirSuperReceiverExpression
        }

    private fun canBeReplaced(callee: FirReference, expression: FirFunctionCall, context: CheckerContext): Boolean {
        // What we want to support:
        // 1. a = a + 1 + 1
        // 2. a = 1 + a + 1
        // 3. a = 1 + 1 + a
        // What we do not:
        // 4. a = (a + 1) + 1

        // Our parsers by default produce right-associative parentheses groupings: a + 1 + 1 => [a + [1 + 1]].
        // Also note that `(a + b)` AST nodes are not binary, though `a + b` are.

        val operatorName = expression.calleeReference.name
        val lefter = expression.explicitReceiver

        when {
            operatorName !in BINARY_OPERATION_NAMES -> return false
            (lefter as? FirQualifiedAccessExpression)?.calleeReference?.symbol == callee.symbol -> return true
        }

        val righter = expression.arguments.firstOrNull() ?: return false
        val isCommutative = operatorName == PLUS || operatorName == TIMES

        return when {
            isCommutative && righter is FirQualifiedAccessExpression && righter.calleeReference.symbol == callee.symbol -> true
            // Without parentheses, `lefter` is definitely not higher, otherwise
            // we wouldn't have parsed the expression as [[a ? b] ? c] in the first place.
            lefter is FirFunctionCall && lefter.isSamePrecedenceAs(operatorName) -> canBeReplaced(callee, lefter, context)
            else -> false
        }
    }

    private fun FirFunctionCall.isSamePrecedenceAs(otherOperator: Name): Boolean {
        return PRECEDENCE_MAP[calleeReference.name]?.ordinal == PRECEDENCE_MAP[otherOperator]?.ordinal
    }
}
