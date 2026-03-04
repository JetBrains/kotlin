/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.backend.handlers.IrValidationErrorChecker
import org.jetbrains.kotlin.test.builders.TwoPhaseTestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives.ESCAPE_MODULE_NAME
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.ApplicationDisposableProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import kotlin.jvm.optionals.getOrNull

abstract class AbstractTwoStageKotlinCompilerTest {
    val configurationBuilder: TwoPhaseTestConfigurationBuilder.() -> Unit = {
        commonConfiguration {
            AbstractKotlinCompilerTest.defaultConfiguration(this)
            defaultDirectives {
                +ESCAPE_MODULE_NAME
            }
            useAdditionalService { createApplicationDisposableProvider() }
            useAdditionalService { createKotlinStandardLibrariesPathProvider() }
            useAfterAnalysisCheckers(::IrValidationErrorChecker)
        }

        nonGroupingPhase {
            startingArtifactFactory = { ResultingArtifact.Source() }
            testInfo = this@AbstractTwoStageKotlinCompilerTest.testInfo
        }

        groupingPhase {
            testInfo = this@AbstractTwoStageKotlinCompilerTest.testInfo
        }

        configure(this)
    }


    private lateinit var testInfo: KotlinTestInfo
    lateinit var nonGroupingRunner: NonGroupingTestRunner
        private set

    var nonGroupingPhaseRunnerInitialized: Boolean = false
        private set

    lateinit var groupingPhaseRunner: GroupingTestRunner
        private set

    var secondPhaseRunnerInitialized: Boolean = false
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

    abstract fun configure(builder: TwoPhaseTestConfigurationBuilder)

    fun initTestRunners(@TestDataFile filePath: String) {
        val configurationBuilder = TwoPhaseTestConfigurationBuilder().apply(configurationBuilder)
        nonGroupingRunner = NonGroupingTestRunner(configurationBuilder.firstPhaseBuilder.build(filePath)).also {
            nonGroupingPhaseRunnerInitialized = true
        }
        groupingPhaseRunner = GroupingTestRunner(configurationBuilder.secondPhaseBuilder.build(filePath)).also {
            secondPhaseRunnerInitialized = true
        }
    }

    fun initTestRunnerAndCreateModuleStructure(@TestDataFile filePath: String) {
        initTestRunners(filePath)
        nonGroupingRunner.prepareModuleStructure(filePath)
    }
}

fun TestInfo.toKotlinTestInfo(): KotlinTestInfo {
    return KotlinTestInfo(
        className = this.testClass.getOrNull()?.name ?: "_undefined_",
        methodName = this.testMethod.getOrNull()?.name ?: "_testUndefined_",
        tags = this.tags
    )
}
