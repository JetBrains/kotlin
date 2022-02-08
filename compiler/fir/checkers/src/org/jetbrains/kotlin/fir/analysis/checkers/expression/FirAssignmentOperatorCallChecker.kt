/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirOperationNameConventions
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit

object FirAssignmentOperatorCallChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val resolvedCalleeSymbol = expression.calleeReference.resolvedSymbol as? FirNamedFunctionSymbol ?: return
        val resolvedCalleeName = resolvedCalleeSymbol.name
        if (expression.origin != FirFunctionCallOrigin.Operator ||
            resolvedCalleeName !in FirOperationNameConventions.ASSIGNMENT_NAMES
        ) {
            return
        }
        if (!expression.typeRef.coneType.isUnit) {
            reporter.reportOn(
                expression.source,
                FirErrors.ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT,
                resolvedCalleeSymbol,
                FirOperationNameConventions.ASSIGNMENT_NAMES[resolvedCalleeName]!!.operator,
                context
            )
        }
    }
}