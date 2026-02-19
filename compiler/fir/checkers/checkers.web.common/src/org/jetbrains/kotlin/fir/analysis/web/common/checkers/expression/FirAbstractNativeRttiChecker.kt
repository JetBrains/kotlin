/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.FirAbstractWebCheckerUtils
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

abstract class FirAbstractNativeRttiChecker(
    private val webCheckerUtils: FirAbstractWebCheckerUtils
) : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        when (expression) {
            is FirGetClassCall -> checkGetClassCall(expression)
            is FirTypeOperatorCall -> checkTypeOperatorCall(expression)
            else -> {}
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGetClassCall(expression: FirGetClassCall) {
        val declarationToCheck = expression.argument.resolvedType.toRegularClassSymbol() ?: return

        if (expression.arguments.firstOrNull() !is FirResolvedQualifier) {
            return
        }

        if (webCheckerUtils.isNativeOrExternalInterface(declarationToCheck, context.session)) {
            reporter.reportOn(expression.source, FirWebCommonErrors.EXTERNAL_INTERFACE_AS_CLASS_LITERAL)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTypeOperatorCall(expression: FirTypeOperatorCall) {
        val targetTypeRef = expression.conversionTypeRef
        val declarationToCheck = targetTypeRef.toRegularClassSymbol(context.session) ?: return

        if (!webCheckerUtils.isNativeOrExternalInterface(declarationToCheck, context.session)) {
            return
        }

        when (expression.operation) {
            FirOperation.AS, FirOperation.SAFE_AS -> reporter.reportOn(
                expression.source,
                FirWebCommonErrors.UNCHECKED_CAST_TO_EXTERNAL_INTERFACE,
                expression.argument.resolvedType,
                targetTypeRef.coneType
            )
            FirOperation.IS, FirOperation.NOT_IS -> reporter.reportOn(
                expression.source,
                FirWebCommonErrors.CANNOT_CHECK_FOR_EXTERNAL_INTERFACE,
                targetTypeRef.coneType
            )
            else -> {}
        }
    }
}
