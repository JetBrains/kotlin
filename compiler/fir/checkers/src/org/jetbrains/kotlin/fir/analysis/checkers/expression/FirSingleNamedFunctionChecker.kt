/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock

object FirSingleNamedFunctionChecker : FirBlockChecker(MppCheckerKind.Common) {
    override fun check(expression: FirBlock, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression is FirSingleExpressionBlock && expression.statement is FirSimpleFunction) {
            reporter.reportOn(
                expression.statement.source,
                FirErrors.SINGLE_ANONYMOUS_FUNCTION_WITH_NAME,
                context
            )
        }
    }
}
