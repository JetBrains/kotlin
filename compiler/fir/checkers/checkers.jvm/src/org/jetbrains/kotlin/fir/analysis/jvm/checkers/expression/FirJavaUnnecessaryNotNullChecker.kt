/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCheckNotNullCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext

object FirJavaUnnecessaryNotNullChecker: FirCheckNotNullCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirCheckNotNullCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val argument = expression.arguments.singleOrNull() ?: return
        val argumentType = EnhancedForWarningConeSubstitutor(context.session.typeContext)
            .substituteOrNull(argument.resolvedType)
            ?.fullyExpandedType(context.session) ?: return

        if (!argumentType.canBeNull(context.session) && context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
            reporter.reportOn(expression.source, FirErrors.UNNECESSARY_NOT_NULL_ASSERTION, argumentType, context)
        }
    }
}
