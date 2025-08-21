/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLLCompilerBasedTest
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.baseFirBlackBoxCodegenTestDirectivesConfiguration
import org.jetbrains.kotlin.test.configuration.configureModernJavaWhenNeeded
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import java.io.File

/**
 * This test case is supposed to check analysis over codegen testData
 * to prevent missed broken analysis (e.g., contract violation).
 * This test case does not interact with the backend at all.
 */
abstract class AbstractLLBlackBoxTestBase : AbstractLLCompilerBasedTest() {
    abstract fun facade(): Constructor<LowLevelFirFrontendFacade>
    abstract fun facadeSpecificSuppressor(): Constructor<AfterAnalysisChecker>

    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            facadeStep(facade())
            baseFirBlackBoxCodegenTestDirectivesConfiguration()
            configureModernJavaWhenNeeded()
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::JvmEnvironmentConfigurator,
            )

            useAdditionalSourceProviders(
                ::AdditionalDiagnosticsSourceFilesProvider,
                ::CoroutineHelpersSourceFilesProvider,
                ::CodegenHelpersSourceFilesProvider,
            )

            firHandlersStep {
                useHandlers(
                    ::FirDiagnosticsHandler,
                    ::FirDumpHandler,
                    ::FirScopeDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirResolvedTypesVerifier,
                )
            }

            useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor, facadeSpecificSuppressor())
        }
    }

    override fun shouldSkipTest(filePath: String, configuration: TestConfiguration): Boolean {
        val testDataFile = File(filePath)
        val targetBackend = TargetBackend.JVM_IR
        if (!InTextDirectivesUtils.isCompatibleTarget(targetBackend, testDataFile)) return true

        return InTextDirectivesUtils.isIgnoredTarget(
            targetBackend,
            testDataFile,
            /*includeAny = */true,
            InTextDirectivesUtils.IGNORE_BACKEND_DIRECTIVE_PREFIX,
            InTextDirectivesUtils.IGNORE_BACKEND_K2_DIRECTIVE_PREFIX,
        )
    }
}
