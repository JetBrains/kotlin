/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.util.isCallableReference
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor

object FunInterfaceConstructorReferenceChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.KotlinFunInterfaceConstructorReference)) return

        val resultingDescriptor = resolvedCall.resultingDescriptor.original
        if (resultingDescriptor !is SamConstructorDescriptor || !resolvedCall.call.isCallableReference()) return

        if (resultingDescriptor.baseDescriptorForSynthetic.isFun) {
            context.trace.report(
                Errors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE.on(reportOn)
            )
        }
    }
}
