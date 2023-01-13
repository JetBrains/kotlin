/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.compiler.visualizer.FirVisualizer
import org.jetbrains.kotlin.compiler.visualizer.PsiVisualizer
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.TestRunner
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractVisualizerBlackBoxTest {
    private fun <O : ResultingArtifact.FrontendOutput<O>> createConfiguration(
        filePath: String,
        frontendKind: FrontendKind<O>,
        frontendFacade: Constructor<FrontendFacade<O>>
    ): TestConfiguration = testConfiguration(filePath) {
        assertions = JUnit5Assertions
        testInfo = KotlinTestInfo("_undefined_", "_testUndefined_", setOf())
        startingArtifactFactory = { ResultingArtifact.Source() }

        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        useSourcePreprocessor(*AbstractKotlinCompilerTest.defaultPreprocessors.toTypedArray())
        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())

        globalDefaults {
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
            frontend = frontendKind
        }

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::CodegenHelpersSourceFilesProvider
        )

        facadeStep(frontendFacade)
    }

    fun runTest(filePath: String) {
        lateinit var psiRenderResult: String
        lateinit var firRenderResult: String

        val psiConfiguration = createConfiguration(filePath, FrontendKinds.ClassicFrontend, ::ClassicFrontendFacade)
        TestRunner(psiConfiguration).runTest(filePath) { testConfiguration ->
            testConfiguration.testServices.moduleStructure.modules.forEach { psiModule ->
                val psiArtifact = testConfiguration.testServices.dependencyProvider.getArtifact(psiModule, FrontendKinds.ClassicFrontend)
                val psiRenderer = psiArtifact.ktFiles.values.firstOrNull()?.let { PsiVisualizer(it, psiArtifact.analysisResult) }
                psiRenderResult = psiRenderer?.render()?.trim() ?: ""
            }
        }

        val firConfiguration = createConfiguration(filePath, FrontendKinds.FIR, ::FirFrontendFacade)
        TestRunner(firConfiguration).runTest(filePath) { testConfiguration ->
            testConfiguration.testServices.moduleStructure.modules.forEach { firModule ->
                val firArtifact = testConfiguration.testServices.dependencyProvider.getArtifact(firModule, FrontendKinds.FIR)
                val firRenderer = firArtifact.mainFirFiles.values.firstOrNull()?.let { FirVisualizer(it) }
                firRenderResult = firRenderer?.render()?.trim() ?: ""
            }
        }

        psiConfiguration.testServices.assertions.assertEquals(psiRenderResult, firRenderResult)
    }
}

