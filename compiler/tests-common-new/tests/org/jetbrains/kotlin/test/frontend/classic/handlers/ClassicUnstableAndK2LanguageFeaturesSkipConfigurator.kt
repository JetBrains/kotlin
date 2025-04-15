/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

// Skip test for K1 in case it explicitly enables any language feature which either has `sinceVersion` equal to 2.*, or is unstable.
class ClassicUnstableAndK2LanguageFeaturesSkipConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        testServices.moduleStructure.modules.first().languageVersionSettings.let {
            if (it.languageVersion.major == 1) {
                it.getManuallyEnabledLanguageFeatures().forEach { feature ->
                    if (feature.sinceVersion?.major == 2 || feature.kind == LanguageFeature.Kind.UNSTABLE_FEATURE)
                        return true
                }
            }
        }
        return false
    }
}
