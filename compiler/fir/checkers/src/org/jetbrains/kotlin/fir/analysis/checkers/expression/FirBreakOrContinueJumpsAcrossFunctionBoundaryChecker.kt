/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*

object FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker : FirLoopJumpChecker(MppCheckerKind.Common) {
    override fun check(expression: FirLoopJump, context: CheckerContext, reporter: DiagnosticReporter) {
        val allowInlined = context.languageVersionSettings.supportsFeature(LanguageFeature.BreakContinueInInlineLambdas)
        val errorPathElements = ArrayDeque<FirElement>()

        fun findPathAndCheck(element: FirElement?): Boolean {
            fun findPathAndCheckWithAddingErrorElement(errorElement: FirElement, checkElement: FirElement?): Boolean {
                errorPathElements.addLast(errorElement)
                val result = findPathAndCheck(checkElement)
                errorPathElements.removeLast()
                return result
            }

            if (element == null) {
                return false
            }

            when (element) {
                expression -> {
                    if (errorPathElements.isNotEmpty()) {
                        val hasNonInline = errorPathElements.any {
                            when(it) {
                                is FirAnonymousFunction -> it.inlineStatus != InlineStatus.Inline
                                is FirAnonymousFunctionExpression -> it.anonymousFunction.inlineStatus != InlineStatus.Inline
                                else -> true
                            }}
                        if (hasNonInline) {
                            reporter.reportOn(expression.source, FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, context)
                        } else if (!allowInlined) {
                            reporter.reportOn(expression.source, FirErrors.UNSUPPORTED_FEATURE,
                                LanguageFeature.BreakContinueInInlineLambdas to context.languageVersionSettings, context)
                        }
                    }
                    return true
                }
                is FirBlock -> {
                    for (statement in element.statements) {
                        if (findPathAndCheck(statement)) {
                            return true
                        }
                    }
                }
                is FirLoop -> return findPathAndCheck(element.condition) || findPathAndCheck(element.block)
                is FirWhenExpression -> {
                    for (branch in element.branches) {
                        if (findPathAndCheck(branch.result)) {
                            return true
                        }
                    }
                }
                is FirVariable -> return findPathAndCheck(element.initializer)
                is FirWrappedExpression -> return findPathAndCheck(element.expression)
                is FirFunctionCall -> {
                    if (findPathAndCheck(element.extensionReceiver) || findPathAndCheck(element.dispatchReceiver)) {
                        return true
                    }
                    if (element.arguments.any(::findPathAndCheck)) return true
                }
                is FirCall -> {
                    for (argument in element.arguments) {
                        if (findPathAndCheck(argument)) {
                            return true
                        }
                    }
                }
                is FirClass -> {
                    errorPathElements.addLast(element)
                    for (declaration in element.declarations) {
                        if (findPathAndCheck(declaration)) {
                            errorPathElements.removeLast()
                            return true
                        }
                    }
                    errorPathElements.removeLast()
                }
                is FirFunction -> {
                    if (findPathAndCheckWithAddingErrorElement(element, element.body)) {
                        return true
                    }

                    if (element is FirConstructor) {
                        val argumentList = element.delegatedConstructor?.argumentList
                        if (argumentList != null) {
                            errorPathElements.addLast(element)
                            for (argument in argumentList.arguments) {
                                if (findPathAndCheck(argument)) {
                                    errorPathElements.removeLast()
                                    return true
                                }
                            }
                            errorPathElements.removeLast()
                        }
                    }
                }
                is FirAnonymousInitializer -> return findPathAndCheckWithAddingErrorElement(element, element.body)
                is FirAnonymousObjectExpression -> return findPathAndCheckWithAddingErrorElement(element, element.anonymousObject)
                is FirAnonymousFunctionExpression -> return findPathAndCheckWithAddingErrorElement(element, element.anonymousFunction)
            }

            return false
        }

        findPathAndCheck(expression.target.labeledElement.block)
    }

}
