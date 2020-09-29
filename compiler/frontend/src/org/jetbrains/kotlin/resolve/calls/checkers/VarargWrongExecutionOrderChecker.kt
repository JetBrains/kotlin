/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.*

object VarargWrongExecutionOrderChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val isCorrectExecutionOrderForVarargArgumentsAlreadyUsed =
            context.languageVersionSettings.getFeatureSupport(LanguageFeature.UseCorrectExecutionOrderForVarargArguments) == LanguageFeature.State.ENABLED

        if (isCorrectExecutionOrderForVarargArgumentsAlreadyUsed) return

        val valueArguments = resolvedCall.call.valueArguments

        val varargIndex = valueArguments.indexOfFirst {
            resolvedCall.getParameterForArgument(it)?.isVararg == true
        }.takeIf { it != -1 } ?: return
        val nonVarargIndex = valueArguments.indexOfLast {
            resolvedCall.getParameterForArgument(it)?.isVararg != true
        }.takeIf { it != -1 } ?: return

        if (varargIndex > nonVarargIndex) return

        val varargValueArgument = valueArguments[varargIndex]

        if (!varargValueArgument.isNamed() || varargValueArgument !is PsiElement) return

        // If named form for vararg is used then we can have only one real value argument on a call site
        context.trace.report(Errors.CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS.on(varargValueArgument))
    }
}
