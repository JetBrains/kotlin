/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.CheckResult
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.OperatorDiagnostic
import org.jetbrains.kotlin.fir.declarations.OperatorFunctionChecks
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.lexer.KtTokens


object FirOperatorModifierChecker : FirFunctionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!declaration.isOperator) return
        //we are not interested in implicit operators from override
        val operatorModifier = declaration.getModifier(KtTokens.OPERATOR_KEYWORD) ?: return

        when (val checkResult = OperatorFunctionChecks.isOperator(declaration, context.session, context.scopeSession)) {
            CheckResult.SuccessCheck -> {}
            CheckResult.IllegalFunctionName -> {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, "illegal function name")
            }
            is CheckResult.IllegalSignature -> {
                checkResult.error.mapOperatorDiagnostic(declaration, operatorModifier.source)
            }
            CheckResult.AnonymousOperatorFunction -> {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, "anonymous function")
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun OperatorDiagnostic.mapOperatorDiagnostic(declaration: FirFunction, modifierSource: KtSourceElement) {
        when (this) {
            is OperatorDiagnostic.DeprecatedOperatorDiagnostic -> {
                val factory =
                    if (feature.isEnabled()) FirErrors.INAPPLICABLE_OPERATOR_MODIFIER
                    else FirErrors.INAPPLICABLE_OPERATOR_MODIFIER_WARNING
                reporter.reportOn(declaration.source, factory, message)
            }
            is OperatorDiagnostic.IllegalOperatorDiagnostic -> {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, message)
            }
            is OperatorDiagnostic.Unsupported -> {
                reporter.reportOn(
                    modifierSource,
                    FirErrors.UNSUPPORTED_FEATURE,
                    feature to context.languageVersionSettings,
                )
            }
            is OperatorDiagnostic.ReturnTypeMismatchWithOuterClass -> {
                when {
                    dueToNullability -> reporter.reportOn(declaration.source, FirErrors.NULLABLE_RETURN_TYPE_OF_OPERATOR_OF)
                    dueToFlexibility -> reporter.reportOn(declaration.source, FirErrors.POTENTIALLY_NULLABLE_RETURN_TYPE_OF_OPERATOR_OF)
                    else -> reporter.reportOn(declaration.source, FirErrors.RETURN_TYPE_MISMATCH_OF_OPERATOR_OF, outer)
                }
            }
        }
    }
}
