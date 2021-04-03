/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testRunner
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.preprocessors.MetaInfosCleanupPreprocessor
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.services.BackendKindExtractor
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.impl.BackendKindExtractorImpl
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.FlexibleTypeImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

abstract class AbstractKotlinCompilerTest {
    companion object {
        val defaultDirectiveContainers = listOf(
            ConfigurationDirectives,
            LanguageSettingsDirectives
        )

        val defaultPreprocessors: List<Constructor<SourceFilePreprocessor>> = listOf(
            ::MetaInfosCleanupPreprocessor
        )

        private fun configureDebugFlags() {
            AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
            FlexibleTypeImpl.RUN_SLOW_ASSERTIONS = true
        }

        val defaultConfiguration: TestConfigurationBuilder.() -> Unit = {
            useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
            useAdditionalService<BackendKindExtractor>(::BackendKindExtractorImpl)
            useSourcePreprocessor(*defaultPreprocessors.toTypedArray())
            useDirectives(*defaultDirectiveContainers.toTypedArray())
            configureDebugFlags()
        }
    }

    private val configuration: TestConfigurationBuilder.() -> Unit = {
        assertions = JUnit5Assertions
        defaultConfiguration()
        configure(this)
    }

    abstract fun TestConfigurationBuilder.configuration()
    private lateinit var testInfo: KotlinTestInfo

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        initTestInfo(
            KotlinTestInfo(
                className = testInfo.testClass.orElseGet(null)?.name ?: "_undefined_",
                methodName = testInfo.testMethod.orElseGet(null)?.name ?: "_testUndefined_",
                tags = testInfo.tags
            )
        )
    }

    fun initTestInfo(testInfo: KotlinTestInfo) {
        this.testInfo = testInfo
    }

    open fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            testInfo = this@AbstractKotlinCompilerTest.testInfo
        }
        builder.configuration()
    }

    open fun runTest(@TestDataFile filePath: String) {
        testRunner(filePath, configuration).runTest(filePath)
    }
}
