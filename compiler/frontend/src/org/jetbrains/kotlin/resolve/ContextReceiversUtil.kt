/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtContextReceiverList
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.containsTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typeUtil.supertypes

fun checkContextReceiversAreEnabled(
    trace: BindingTrace,
    languageVersionSettings: LanguageVersionSettings,
    contextReceiverList: KtContextReceiverList
) {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
        trace.report(
            Errors.UNSUPPORTED_FEATURE.on(
                contextReceiverList,
                LanguageFeature.ContextReceivers to languageVersionSettings
            )
        )
    }
}

fun checkSubtypingBetweenContextReceivers(
    trace: BindingTrace,
    contextReceiverList: KtContextReceiverList,
    contextReceiverTypes: List<KotlinType>
) {
    fun KotlinType.prepared(): KotlinType = when {
        isTypeParameter() -> supertypes().first()
        containsTypeParameter() -> replaceArgumentsWithStarProjections()
        else -> this
    }
    for (i in 0 until contextReceiverTypes.lastIndex) {
        val contextReceiverType = contextReceiverTypes[i].prepared()
        for (j in (i + 1) until contextReceiverTypes.size) {
            val anotherContextReceiverType = contextReceiverTypes[j].prepared()
            if (NewKotlinTypeChecker.Default.isSubtypeOf(contextReceiverType, anotherContextReceiverType) ||
                NewKotlinTypeChecker.Default.isSubtypeOf(anotherContextReceiverType, contextReceiverType)
            ) {
                trace.report(Errors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS.on(contextReceiverList))
                return
            }
        }
    }
}

