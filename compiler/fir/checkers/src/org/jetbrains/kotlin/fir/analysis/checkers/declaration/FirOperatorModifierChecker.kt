/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.CheckResult
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.OperatorFunctionChecks
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.lexer.KtTokens


object FirOperatorModifierChecker : FirFunctionChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isOperator) return
        //we are not interested in implicit operators from override
        if (!declaration.hasModifier(KtTokens.OPERATOR_KEYWORD)) return

        when (val checkResult = OperatorFunctionChecks.isOperator(declaration, context.session, context.scopeSession)) {
            CheckResult.SuccessCheck -> {}
            CheckResult.IllegalFunctionName -> {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, "illegal function name", context)
            }
            is CheckResult.IllegalSignature -> {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, checkResult.error, context)
            }
            CheckResult.AnonymousOperatorFunction -> {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, "anonymous function", context)
            }
        }
    }
}
