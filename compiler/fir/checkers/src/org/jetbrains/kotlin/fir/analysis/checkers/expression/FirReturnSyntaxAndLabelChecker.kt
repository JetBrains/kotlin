/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

object FirReturnSyntaxAndLabelChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        val source = expression.source
        if (source?.kind is KtFakeSourceElementKind.ImplicitReturn) return

        val labeledElement = expression.target.labeledElement
        val targetSymbol = labeledElement.symbol
        if (labeledElement is FirErrorFunction && (labeledElement.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.NotAFunctionLabel) {
            reporter.reportOn(source, FirErrors.NOT_A_FUNCTION_LABEL)
        } else if (labeledElement is FirErrorFunction && (labeledElement.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.UnresolvedLabel) {
            reporter.reportOn(source, FirErrors.UNRESOLVED_LABEL)
        } else if (!isReturnAllowed(targetSymbol, context)) {
            reporter.reportOn(source, FirErrors.RETURN_NOT_ALLOWED)
        }

        if (targetSymbol is FirAnonymousFunctionSymbol) {
            val label = targetSymbol.label
            if (label?.source?.kind !is KtRealSourceElementKind) {
                val functionCall = context.callsOrAssignments.asReversed().find {
                    it is FirFunctionCall &&
                            (it.calleeReference.toResolvedNamedFunctionSymbol())?.callableId ==
                            FirSuspendCallChecker.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_CALLABLE_ID
                }
                if (functionCall is FirFunctionCall &&
                    functionCall.arguments.any {
                        it is FirAnonymousFunctionExpression && it.anonymousFunction.symbol == targetSymbol
                    }
                ) {
                    reporter.reportOn(source, FirErrors.RETURN_FOR_BUILT_IN_SUSPEND)
                }
            }
        }

        val containingDeclaration = context.containingDeclarations.last()
        if (containingDeclaration is FirFunction &&
            containingDeclaration.body is FirSingleExpressionBlock &&
            containingDeclaration.source?.kind != KtFakeSourceElementKind.DelegatedPropertyAccessor
        ) {
            reporter.reportOn(source, FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY)
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
                is FirValueParameter -> return false
                else -> {}
            }
        }
        return true
    }
}
