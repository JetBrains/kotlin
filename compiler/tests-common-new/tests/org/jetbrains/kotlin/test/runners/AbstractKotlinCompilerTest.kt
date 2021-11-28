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
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.preprocessors.MetaInfosCleanupPreprocessor
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
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
            useSourcePreprocessor(*defaultPreprocessors.toTypedArray())
            useDirectives(*defaultDirectiveContainers.toTypedArray())
            configureDebugFlags()
            startingArtifactFactory = { ResultingArtifact.Source() }
        }
    }

    protected val configuration: TestConfigurationBuilder.() -> Unit = {
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

    @JvmOverloads
    open fun runTest(
        @TestDataFile filePath: String,
        actualContentModifier: (String) -> String,
        expectedContentModifier: ((String) -> String)? = actualContentModifier
    ) {
        class SourceTransformer(testServices: TestServices) : SourceFilePreprocessor(testServices) {
            override fun process(file: TestFile, content: String): String = content.let(actualContentModifier)
        }
        testRunner(filePath) {
            configuration.let { it() } // property configuration, not method
            useSourcePreprocessor({ SourceTransformer(it) })
        }.runTest(filePath, expectedFileTransformer = expectedContentModifier)
    }
}
