/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.CallableReferenceKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallArgument
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.psiCallArgument
import org.jetbrains.kotlin.resolve.calls.tower.psiExpression

object SuspendConversionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall !is NewResolvedCallImpl<*>) return

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.SuspendConversion)) return

        val argumentsWithSuspendConversion = resolvedCall.resolvedCallAtom.argumentsWithSuspendConversion

        for (argumentWithSuspendConversion in argumentsWithSuspendConversion.keys) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    argumentWithSuspendConversion.psiCallArgument.valueArgument.asElement(),
                    LanguageFeature.SuspendConversion to context.languageVersionSettings
                )
            )
        }
    }
}