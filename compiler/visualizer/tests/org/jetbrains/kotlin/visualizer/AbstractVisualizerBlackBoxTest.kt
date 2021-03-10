/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TestRunner
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.BackendKindExtractorImpl
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractVisualizerBlackBoxTest {
    private val commonConfiguration: TestConfigurationBuilder.() -> Unit = {
        assertions = JUnit5Assertions
        testInfo = KotlinTestInfo("_undefined_", "_testUndefined_", setOf())

        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        useAdditionalService<BackendKindExtractor>(::BackendKindExtractorImpl)
        useSourcePreprocessor(*AbstractKotlinCompilerTest.defaultPreprocessors.toTypedArray())
        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())

        globalDefaults {
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.WITH_STDLIB
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )

        useFrontendFacades(
            ::ClassicFrontendFacade,
            ::FirFrontendFacade
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::CodegenHelpersSourceFilesProvider
        )
    }

    fun runTest(filePath: String) {
        val psiConfiguration = testConfiguration(filePath) {
            commonConfiguration()
            globalDefaults { frontend = FrontendKinds.ClassicFrontend }
        }
        TestRunner(psiConfiguration).runTest(filePath)

        val firConfiguration = testConfiguration(filePath) {
            commonConfiguration()
            globalDefaults { frontend = FrontendKinds.FIR }
        }
        TestRunner(firConfiguration).runTest(filePath)

        VisualizerBlackBoxHandler(psiConfiguration.testServices, firConfiguration.testServices).process()
    }
}

