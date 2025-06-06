/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasExplicitReturnType
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*

object FirReturnSyntaxAndLabelChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        val source = expression.source
        if (source?.kind is KtFakeSourceElementKind.ImplicitReturn || source?.kind is KtFakeSourceElementKind.DelegatedPropertyAccessor) return

        val labeledElement = expression.target.labeledElement
        val targetSymbol = labeledElement.symbol

        when (((labeledElement as? FirErrorFunction)?.diagnostic as? ConeSimpleDiagnostic)?.kind) {
            DiagnosticKind.NotAFunctionLabel -> FirErrors.NOT_A_FUNCTION_LABEL
            DiagnosticKind.UnresolvedLabel -> FirErrors.UNRESOLVED_LABEL
            else -> returnNotAllowedFactoryOrNull(targetSymbol)
        }?.let {
            reporter.reportOn(source, it)
        }

        checkBuiltInSuspend(targetSymbol, source)
    }

    context(context: CheckerContext)
    private fun FirFunction.supportsReturnExpression(): Boolean {
        return body !is FirSingleExpressionBlock ||
                symbol.hasExplicitReturnType &&
                context.languageVersionSettings.supportsFeature(LanguageFeature.AllowReturnInExpressionBodyWithExplicitType)
    }

    context(context: CheckerContext)
    private fun returnNotAllowedFactoryOrNull(targetSymbol: FirFunctionSymbol<*>): KtDiagnosticFactory0? {
        var existingFalseNegative = false

        for (containingDeclaration in context.containingDeclarations.asReversed()) {
            @OptIn(SymbolInternals::class)
            when (containingDeclaration) {
                // return from member of local class or anonymous object
                is FirClassSymbol -> {
                    return FirErrors.RETURN_NOT_ALLOWED
                }
                is FirFunctionSymbol if (containingDeclaration == targetSymbol) -> {
                    val targetFunction = targetSymbol.fir
                    return FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.takeUnless { existingFalseNegative || targetFunction.supportsReturnExpression() }
                }
                is FirAnonymousFunctionSymbol -> {
                    if (!containingDeclaration.inlineStatus.returnAllowed) {
                        return FirErrors.RETURN_NOT_ALLOWED
                    } else {
                        existingFalseNegative = true
                    }
                }
                is FirFunctionSymbol -> {
                    return FirErrors.RETURN_NOT_ALLOWED
                }
                is FirPropertySymbol -> {
                    if (!containingDeclaration.isLocal) {
                        return FirErrors.RETURN_NOT_ALLOWED
                    } else {
                        existingFalseNegative = true
                    }
                }
                is FirValueParameterSymbol -> {
                    return FirErrors.RETURN_NOT_ALLOWED
                }
                else -> {}
            }
        }
        return null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBuiltInSuspend(
        targetSymbol: FirFunctionSymbol<FirFunction>,
        source: KtSourceElement?,
    ) {
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
    }
}
