/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

object FirReturnSyntaxAndLabelChecker : FirReturnExpressionChecker() {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = expression.source
        if (source?.kind == KtFakeSourceElementKind.ImplicitReturn) return

        val labeledElement = expression.target.labeledElement
        val targetSymbol = labeledElement.symbol
        if (labeledElement is FirErrorFunction && (labeledElement.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.NotAFunctionLabel) {
            reporter.reportOn(source, FirErrors.NOT_A_FUNCTION_LABEL, context)
        } else if (!isReturnAllowed(targetSymbol, context)) {
            reporter.reportOn(source, FirErrors.RETURN_NOT_ALLOWED, context)
        }

        if (targetSymbol is FirAnonymousFunctionSymbol) {
            val label = targetSymbol.label
            if (label?.source?.kind !is KtRealSourceElementKind) {
                val functionCall = context.qualifiedAccessOrAnnotationCalls.asReversed().find {
                    it is FirFunctionCall &&
                            ((it.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)?.callableId ==
                            FirSuspendCallChecker.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_CALLABLE_ID
                }
                if (functionCall is FirFunctionCall &&
                    functionCall.arguments.any {
                        it is FirLambdaArgumentExpression &&
                                (it.expression as? FirAnonymousFunctionExpression)?.anonymousFunction?.symbol == targetSymbol
                    }
                ) {
                    reporter.reportOn(source, FirErrors.RETURN_FOR_BUILT_IN_SUSPEND, context)
                }
            }
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
                else -> {}
            }
        }
        return true
    }
}
