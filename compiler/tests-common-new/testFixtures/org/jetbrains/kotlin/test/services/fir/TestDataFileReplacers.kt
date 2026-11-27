/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.fir

import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_FEATURE_TOGGLED
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.baseTestDataFile
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.jetbrains.kotlin.test.utils.lfDisabledTestDataFile
import java.io.File

/**
 * Skips the tests without [LANGUAGE_FEATURE_TOGGLED]: only their language feature can be disabled.
 */
class LanguageFeatureDisabledMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        // The test data rather than the variant the test reads, so that a variant diverging from it is still checked
        return testServices.baseTestDataFile.useLines { lines ->
            lines.none { RegisteredDirectivesParser.parseDirective(it)?.name == LANGUAGE_FEATURE_TOGGLED.name }
        }
    }
}

/**
 * Makes a test read the `.disabled.kt` variant of its test data if it exists. A test implementing `ManagedTest` with the `disabled` variant
 * reads it by its variant chain instead.
 */
class LanguageFeatureDisabledTestDataConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val disabledTestDataFile = File(testDataFileName).lfDisabledTestDataFile
        return if (disabledTestDataFile.exists()) disabledTestDataFile.path else testDataFileName
    }
}
