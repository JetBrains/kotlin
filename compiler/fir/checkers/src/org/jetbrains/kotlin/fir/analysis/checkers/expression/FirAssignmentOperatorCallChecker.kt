/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature.AssignOperatorOverload
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirOperationNameConventions
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirAssignmentOperatorCallChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val resolvedCalleeSymbol = expression.calleeReference.resolvedSymbol as? FirNamedFunctionSymbol ?: return
        if (expression.origin != FirFunctionCallOrigin.Operator) return
        val resolvedCalleeName = resolvedCalleeSymbol.name
        val operator = FirOperationNameConventions.getOperatorOrNull(resolvedCalleeName, context) ?: return
        if (!expression.typeRef.coneType.isUnit) {
            reporter.reportOn(
                expression.source,
                FirErrors.ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT,
                resolvedCalleeSymbol,
                operator,
                context
            )
        }
    }

    private fun FirOperationNameConventions.getOperatorOrNull(resolvedCalleeName: Name, context: CheckerContext): String? {
        val operator = ASSIGNMENT_NAMES[resolvedCalleeName]?.operator
        if (operator != null) {
            return operator
        }
        return getAssignOperatorIfSupported(resolvedCalleeName, context)
    }

    private fun getAssignOperatorIfSupported(resolvedCalleeName: Name, context: CheckerContext): String? {
        return if (resolvedCalleeName == OperatorNameConventions.ASSIGN
            && context.languageVersionSettings.supportsFeature(AssignOperatorOverload)
        ) {
            return FirOperation.ASSIGN.operator
        } else {
            null
        }
    }
}