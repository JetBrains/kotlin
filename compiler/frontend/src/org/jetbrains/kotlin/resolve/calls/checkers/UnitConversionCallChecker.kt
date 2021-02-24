/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.psiCallArgument

object UnitConversionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall !is NewResolvedCallImpl<*>) return

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.UnitConversion)) return

        // lambdas are working since 1.0, callable references are handled as part of reference adaptation
        // => here we're checking only simple argument
        val argumentsWithUnitConversion = resolvedCall.resolvedCallAtom.argumentsWithUnitConversion

        for (argumentWithUnitConversion in argumentsWithUnitConversion.keys) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    argumentWithUnitConversion.psiCallArgument.valueArgument.asElement(),
                    LanguageFeature.UnitConversion to context.languageVersionSettings
                )
            )
        }
    }
}