/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.smartIsSubtype
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirFunctionReturnTypeMismatchChecker : FirReturnExpressionChecker() {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.source ?: return
        val targetElement = expression.target.labeledElement
        if (targetElement !is FirSimpleFunction) return

        val functionReturnType = targetElement.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val typeContext = context.session.typeContext
        val returnExpressionType = expression.result.typeRef.coneTypeSafe<ConeKotlinType>() ?: return

        if (!smartIsSubtype(returnExpressionType, functionReturnType, typeContext)) {
            val returnExpressionSource = expression.result.source ?: return
            reporter.report(RETURN_TYPE_MISMATCH.on(returnExpressionSource, functionReturnType, returnExpressionType), context)
        }
    }
}