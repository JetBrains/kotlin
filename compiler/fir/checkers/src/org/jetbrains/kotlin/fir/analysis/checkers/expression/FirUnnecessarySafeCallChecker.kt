/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit

object FirUnnecessarySafeCallChecker : FirSafeCallExpressionChecker() {
    override fun check(expression: FirSafeCallExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val receiverType = expression.receiver.typeRef.coneType.fullyExpandedType(context.session)
        if (receiverType.isUnit || expression.receiver.source?.elementType == KtNodeTypes.SUPER_EXPRESSION) {
            reporter.reportOn(expression.source, FirErrors.UNEXPECTED_SAFE_CALL, context)
            return
        }
        if (!receiverType.canBeNull) {
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
                reporter.reportOn(expression.source, FirErrors.UNNECESSARY_SAFE_CALL, receiverType, context)
            }
            if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)) {
                reporter.reportOn(expression.source, FirErrors.SAFE_CALL_WILL_CHANGE_NULLABILITY, context)
            }
        }
    }
}
