/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isSyntheticEnumEntries

object EnumEntriesUnsupportedChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val languageVersionSettings = context.languageVersionSettings
        if (languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)) return
        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return
        if (propertyDescriptor.isSyntheticEnumEntries()) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    reportOn,
                    LanguageFeature.EnumEntries to languageVersionSettings
                )
            )
        }
    }
}