/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object PolymorphicSignatureCallChecker : CallChecker {
    @JvmField
    val polymorphicSignatureFqName = FqName("java.lang.invoke.MethodHandle.PolymorphicSignature")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.PolymorphicSignature)) return
        if (!resolvedCall.resultingDescriptor.annotations.hasAnnotation(polymorphicSignatureFqName)) return

        for (valueArgument in resolvedCall.valueArgumentsByIndex ?: return) {
            if (valueArgument !is VarargValueArgument) continue
            for (argument in valueArgument.arguments) {
                val spread = argument.getSpreadElement() ?: continue
                context.trace.report(ErrorsJvm.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL.on(context.languageVersionSettings, spread))
            }
        }
    }
}
