/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.AbstractTwoAttributesMetaInfoProcessor

class OldNewInferenceMetaInfoProcessor(testServices: TestServices) : AbstractTwoAttributesMetaInfoProcessor(testServices) {
    companion object {
        const val OI = "OI"
        const val NI = "NI"
    }

    override val firstAttribute: String get() = NI
    override val secondAttribute: String get() = OI

    override fun processorEnabled(module: TestModule): Boolean {
        return DiagnosticsDirectives.WITH_NEW_INFERENCE in module.directives
    }

    override fun firstAttributeEnabled(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
    }
}
