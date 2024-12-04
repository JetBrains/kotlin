/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker.DeclarationKind.InlinedLambda
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker.DeclarationKind.NotInalienableDeclaration
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*

object FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker : FirLoopJumpChecker(MppCheckerKind.Common) {
    override fun check(expression: FirLoopJump, context: CheckerContext, reporter: DiagnosticReporter) {
        val targetElement = expression.target.labeledElement.block
        val path = context.containingElements.dropWhile { it != targetElement }
        val inlineLambdasSupported = context.languageVersionSettings.supportsFeature(LanguageFeature.BreakContinueInInlineLambdas)

        val notTransparentDeclarationKinds = path.mapNotNull {
            when (it) {
                is FirAnonymousFunction -> when (it.inlineStatus) {
                    InlineStatus.Inline -> InlinedLambda
                    else -> NotInalienableDeclaration
                }
                is FirFunction, is FirClass -> NotInalienableDeclaration
                else -> null
            }
        }

        when {
            notTransparentDeclarationKinds.isEmpty() -> {}
            notTransparentDeclarationKinds.any { it == NotInalienableDeclaration } -> {
                reporter.reportOn(expression.source, FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, context)
            }
            !inlineLambdasSupported -> {
                reporter.reportOn(
                    expression.source, FirErrors.UNSUPPORTED_FEATURE,
                    LanguageFeature.BreakContinueInInlineLambdas to context.languageVersionSettings, context
                )
            }
        }
    }

    private enum class DeclarationKind {
        InlinedLambda, NotInalienableDeclaration
    }
}
