/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.getCustomizedEffectivelyEnabledLanguageFeatures
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

// Skip test for K1 in case it explicitly enables any language feature which either has `sinceVersion` equal to 2.*, or is unstable without sinceVersion.
class ClassicUnstableAndK2LanguageFeaturesSkipConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    companion object {
        private val unscheduledK2OnlyFeatures = setOf(
            LanguageFeature.IrInlinerBeforeKlibSerialization,
            LanguageFeature.ContextParameters,
            LanguageFeature.ContractSyntaxV2,
            LanguageFeature.ExplicitBackingFields,
            LanguageFeature.AnnotationAllUseSiteTarget,
            LanguageFeature.ImplicitJvmExposeBoxed,
            LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface,
            LanguageFeature.NameBasedDestructuring,
            LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses,
            LanguageFeature.EnableNameBasedDestructuringShortForm,
        )
    }

    override fun shouldSkipTest(): Boolean {
        val settings = testServices.moduleStructure.modules.first().languageVersionSettings
        if (settings.languageVersion.usesK2) return false
        return settings.getCustomizedEffectivelyEnabledLanguageFeatures().any { feature ->
            when (val sinceVersion = feature.sinceVersion) {
                null -> feature in unscheduledK2OnlyFeatures
                else -> sinceVersion.usesK2
            }
        }
    }
}
