/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicit
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.ownTypeArguments

object FirTypeArgumentsNotAllowedExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val explicitReceiver = expression.explicitReceiver?.unwrapSmartcastExpression()

        if (explicitReceiver is FirResolvedQualifier) {
            val qualifierWithTypeArguments = explicitReceiver.lastQualifierPartWithTypeArguments() ?: return

            if (explicitReceiver.symbol == null) {
                reporter.reportOn(explicitReceiver.source, FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED, "for packages")
            }

            if (explicitReceiver.symbol != null && expression.isStaticMemberCall()) {
                val diagnostic = if (LanguageFeature.ForbidUselessTypeArgumentsIn25.isEnabled()) {
                    FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED
                } else {
                    FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED_WARNING
                }

                reporter.reportOn(
                    qualifierWithTypeArguments.source,
                    diagnostic,
                    "when static member is accessed",
                    positioningStrategy = SourceElementPositioningStrategies.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER,
                )
            }
        }

        if (
            expression is FirImplicitInvokeCall && explicitReceiver is FirPropertyAccessExpression &&
            expression.typeArguments.any { it.isExplicit } &&
            expression.toResolvedCallableSymbol()?.typeParameterSymbols?.isNotEmpty() == true &&
            explicitReceiver.toResolvedCallableSymbol()?.typeParameterSymbols?.isNotEmpty() == true
        ) {
            reporter.reportOn(expression.calleeReference.source, FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED, "on implicit invoke call")
            return
        }
    }

    private tailrec fun FirResolvedQualifier.lastQualifierPartWithTypeArguments(): FirResolvedQualifier? {
        if (typeArguments.isEmpty()) return null
        if (!ownTypeArguments.isEmpty()) return this

        return explicitParent?.lastQualifierPartWithTypeArguments()
    }

    private fun FirQualifiedAccessExpression.isStaticMemberCall(): Boolean {
        return when (this) {
            is FirFunctionCall, is FirPropertyAccessExpression -> {
                toResolvedCallableSymbol()?.isStatic == true
            }
            else -> false
        }
    }
}
