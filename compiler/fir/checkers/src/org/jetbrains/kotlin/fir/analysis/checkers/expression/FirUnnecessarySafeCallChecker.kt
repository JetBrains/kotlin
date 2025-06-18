/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.resolvedType

abstract class AbstractFirUnnecessarySafeCallChecker : FirSafeCallExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected fun checkSafeCallReceiverType(
        receiverType: ConeKotlinType,
        source: KtSourceElement?,
    ) {
        if (!receiverType.canBeNull(context.session)) {
            if (LanguageFeature.EnableDfaWarningsInK2.isEnabled()) {
                reporter.reportOn(source, FirErrors.UNNECESSARY_SAFE_CALL, receiverType)
            }
        }
    }
}

object FirUnnecessarySafeCallChecker : AbstractFirUnnecessarySafeCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirSafeCallExpression) {
        val receiverType = expression.receiver.resolvedType.fullyExpandedType()
        if (expression.receiver.source?.elementType == KtNodeTypes.SUPER_EXPRESSION) {
            reporter.reportOn(expression.source, FirErrors.UNEXPECTED_SAFE_CALL)
            return
        }
        checkSafeCallReceiverType(receiverType, expression.source)
    }
}
