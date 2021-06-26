/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

object FirReturnAllowedChecker : FirReturnExpressionChecker() {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = expression.source
        if (source?.kind == FirFakeSourceElementKind.ImplicitReturn) return

        val targetSymbol = expression.target.labeledElement.symbol

        if (!isReturnAllowed(targetSymbol, context)) {
            reporter.reportOn(source, FirErrors.RETURN_NOT_ALLOWED, context)
        }

        val containingDeclaration = context.containingDeclarations.last()
        if (containingDeclaration is FirFunction && containingDeclaration.body is FirSingleExpressionBlock) {
            reporter.reportOn(source, FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY, context)
        }
    }

    private fun isReturnAllowed(targetSymbol: FirFunctionSymbol<*>, context: CheckerContext): Boolean {
        for (containingDeclaration in context.containingDeclarations.asReversed()) {
            when (containingDeclaration) {
                // return from member of local class or anonymous object
                is FirClass -> return false
                is FirFunction -> {
                    when {
                        containingDeclaration.symbol == targetSymbol -> return true
                        containingDeclaration is FirAnonymousFunction -> {
                            if (!containingDeclaration.inlineStatus.returnAllowed) return false
                        }
                        else -> return false
                    }
                }
                is FirProperty -> if (!containingDeclaration.isLocal) return false
                is FirValueParameter -> return true
            }
        }
        return true
    }
}
