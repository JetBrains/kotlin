/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.GroupingTestRunner
import org.jetbrains.kotlin.test.NonGroupingTestRunner
import org.jetbrains.kotlin.test.backend.handlers.IrValidationErrorChecker
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.toKotlinTestInfo
import org.jetbrains.kotlin.test.services.ApplicationDisposableProvider
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

abstract class AbstractTwoStageKotlinCompilerTest : AbstractTwoStageKotlinCompilerTestBase() {
    val configurationBuilder: TwoStageTestConfigurationBuilder.() -> Unit = {
        commonConfiguration {
            AbstractKotlinCompilerTest.defaultConfiguration(this)
            defaultDirectives {
                +ModuleStructureDirectives.ESCAPE_MODULE_NAME
            }
            useAdditionalService { createApplicationDisposableProvider() }
            useAdditionalService { createKotlinStandardLibrariesPathProvider() }
            useSourcePreprocessor(::BatchingPackageInserter)
            useFailureSuppressors(::IrValidationErrorChecker)
        }

        nonGroupingStage {
            startingArtifactFactory = { ResultingArtifact.Source() }
            testInfo = this@AbstractTwoStageKotlinCompilerTest.testInfo
            useGroupingTestIsolators(::MutedTestsIsolator)
        }

        groupingStage {
            testInfo = this@AbstractTwoStageKotlinCompilerTest.testInfo
        }

        configure(this)
    }


    private lateinit var testInfo: KotlinTestInfo
    final override lateinit var nonGroupingRunner: NonGroupingTestRunner
        private set

    final override var nonGroupingStageRunnerInitialized: Boolean = false
        private set

    final override lateinit var groupingStageRunner: GroupingTestRunner
        private set

    final override var secondStageRunnerInitialized: Boolean = false
        private set

    open fun createApplicationDisposableProvider(): ApplicationDisposableProvider {
        return ExecutionListenerBasedDisposableProvider()
    }

    open fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return StandardLibrariesPathProviderForKotlinProject
    }

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        initTestInfo(testInfo.toKotlinTestInfo())
    }

    fun initTestInfo(testInfo: KotlinTestInfo) {
        this.testInfo = testInfo
    }

    abstract fun configure(builder: TwoStageTestConfigurationBuilder)

    fun initTestRunners(@TestDataFile filePath: String) {
        val configurationBuilder = TwoStageTestConfigurationBuilder().apply(configurationBuilder)
        nonGroupingRunner = NonGroupingTestRunner(configurationBuilder.nonGroupingStageBuilder.build(filePath)).also {
            nonGroupingStageRunnerInitialized = true
        }
        groupingStageRunner = GroupingTestRunner(configurationBuilder.groupingStageBuilder.build(filePath)).also {
            secondStageRunnerInitialized = true
        }
    }

    fun initTestRunnerAndCreateModuleStructure(@TestDataFile filePath: String) {
        initTestRunners(filePath)
        nonGroupingRunner.prepareModuleStructure(filePath)
    }
}
