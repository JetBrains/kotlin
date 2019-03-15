/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.types.typeUtil.contains

object BigFunctionTypeAvailabilityChecker : ClassifierUsageChecker {
    override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.FunctionTypesWithBigArity)) return

        if (targetDescriptor.defaultType.contains { argumentType ->
            val descriptor = argumentType.constructor.declarationDescriptor
            descriptor is FunctionClassDescriptor && descriptor.hasBigArity
        }) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    element, LanguageFeature.FunctionTypesWithBigArity to context.languageVersionSettings
                )
            )
        }
    }
}
