/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.name.StandardClassIds

object FirContractNotFirstStatementChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (StandardClassIds.Callables.contract != expression.toResolvedCallableSymbol()?.callableId) return

        val containingDeclaration = context.containingDeclarations.last()
        if (!(containingDeclaration is FirFunction && expression.isCorrectlyPlacedIn(containingDeclaration))) {
            val message = if (containingDeclaration is FirFunction && containingDeclaration.body is FirSingleExpressionBlock) {
                "Contracts are only allowed in function body blocks."
            } else {
                "Contract should be the first statement."
            }

            reporter.reportOn(expression.source, FirErrors.CONTRACT_NOT_ALLOWED, message, context)
        }
    }

    private fun FirFunctionCall.isCorrectlyPlacedIn(functionDeclaration: FirFunction): Boolean {
        val firstStatement = functionDeclaration.body?.statements?.first()
        return firstStatement is FirContractCallBlock && firstStatement.call == this
    }
}
