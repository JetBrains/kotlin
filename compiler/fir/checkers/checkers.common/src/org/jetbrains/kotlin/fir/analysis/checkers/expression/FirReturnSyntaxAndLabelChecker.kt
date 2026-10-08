/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature.AllowReturnInExpressionBodyWithExplicitType
import org.jetbrains.kotlin.config.LanguageFeature.ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases
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
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.StandardClassIds

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

        if (AllowReturnInExpressionBodyWithExplicitType.isEnabled() &&
            targetSymbol.expressionBodyOrNull()?.statement.let { it is FirReturnExpression && it.result == expression } &&
            targetSymbol.hasExplicitReturnType
        ) {
            reporter.reportOn(source, FirErrors.REDUNDANT_RETURN)
        }
    }

    context(context: CheckerContext)
    private fun returnNotAllowedFactoryOrNull(targetSymbol: FirFunctionSymbol<*>): KtDiagnosticFactory0? {
        var existingFalseNegative = false

        for (containingDeclaration in context.containingDeclarations.asReversed()) {
            when (containingDeclaration) {
                // return from member of local class or anonymous object
                is FirClassSymbol -> {
                    return FirErrors.RETURN_NOT_ALLOWED
                }
                is FirFunctionSymbol if (containingDeclaration == targetSymbol) -> {
                    return returnNotAllowedInExpressionBodyFactoryOrNull(targetSymbol, existingFalseNegative)
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
                    if (containingDeclaration is FirRegularPropertySymbol) {
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

    context(context: CheckerContext)
    private fun returnNotAllowedInExpressionBodyFactoryOrNull(
        targetSymbol: FirFunctionSymbol<*>,
        edgeCase: Boolean,
    ): KtDiagnosticFactory0? {
        // No expression body, all good
        if (targetSymbol.expressionBodyOrNull() == null) return null

        val allowWithExplicitType = AllowReturnInExpressionBodyWithExplicitType.isEnabled()

        if ((allowWithExplicitType || edgeCase) && targetSymbol.hasExplicitReturnType) return null

        val forbidEdgeCases = ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases.isEnabled() && allowWithExplicitType

        return when {
            // Phase 3: Forbid edge cases and report new error
            forbidEdgeCases -> FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE
            // Phase 2: Report warning on edge cases, new error in other cases
            allowWithExplicitType && edgeCase -> FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING
            allowWithExplicitType && !edgeCase -> FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE
            // Phase 1: Ignore edge cases, report old error in other cases
            edgeCase -> null
            else -> FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirFunctionSymbol<*>.expressionBodyOrNull(): FirSingleExpressionBlock? {
        return fir.body as? FirSingleExpressionBlock
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
                            StandardClassIds.Callables.suspend
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
