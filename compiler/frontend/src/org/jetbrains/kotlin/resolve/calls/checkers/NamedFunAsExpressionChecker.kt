/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors.ANONYMOUS_FUNCTION_WITH_NAME
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.isFunctionalExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


/**
 * That checker used to check that only anonymous functions are used as function arguments
 *
 * Examples:
 *
 *   fun foo(obj: Any) {}
 *   foo(fun named() {}) // bad
 *
 *  // here `if` is synthetic function call
 *   val x = if (b) fun named1() {} else fun named2() {}
 */
object NamedFunAsExpressionChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return
        for (argument in resolvedCall.valueArguments.values.filterIsInstance(ExpressionValueArgument::class.java)) {
            val expression = KtPsiUtil.deparenthesize(argument.valueArgument?.getArgumentExpression()) as? KtNamedFunction ?: continue
            if (!expression.isFunctionalExpression()) {
                /*
                 * There is one another place, where ANONYMOUS_FUNCTION_WITH_NAME is reported
                 *   ([org.jetbrains.kotlin.types.expressions.FunctionsTypingVisitor]), so in some cases there is
                 *   can be duplicated diagnostic. It is a little problem that should go when we will resolve assignments
                 *   as calls, so that checker will report all cases of ANONYMOUS_FUNCTION_WITH_NAME diagnostic.
                 */
                context.trace.reportDiagnosticOnce(ANONYMOUS_FUNCTION_WITH_NAME.on(expression.nameIdentifier!!))
            }
        }
    }
}