/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.builders.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testRunner
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.preprocessors.MetaInfosCleanupPreprocessor
import org.jetbrains.kotlin.test.services.BackendKindExtractor
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.impl.BackendKindExtractorImpl
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.FlexibleTypeImpl

abstract class AbstractKotlinCompilerTest {
    companion object {
        val defaultDirectiveContainers = listOf(
            ConfigurationDirectives,
            LanguageSettingsDirectives
        )

        val defaultPreprocessors: List<Constructor<SourceFilePreprocessor>> = listOf(
            ::MetaInfosCleanupPreprocessor
        )
    }

    private val configuration: TestConfigurationBuilder.() -> Unit = {
        assertions = JUnit5Assertions
        useAdditionalService<BackendKindExtractor>(::BackendKindExtractorImpl)
        useSourcePreprocessor(*defaultPreprocessors.toTypedArray())
        useDirectives(*defaultDirectiveContainers.toTypedArray())
        configureDebugFlags()
        configure(this)
    }

    private fun configureDebugFlags() {
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
        FlexibleTypeImpl.RUN_SLOW_ASSERTIONS = true
    }

    abstract fun TestConfigurationBuilder.configuration()

    open fun configure(builder: TestConfigurationBuilder) {
        builder.configuration()
    }

    fun runTest(@TestDataFile filePath: String) {
        testRunner(filePath, configuration).runTest(filePath)
    }
}
