/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator

abstract class AbstractFirDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            backend = BackendKind.NoBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        enableMetaInfoHandler()

        useConfigurators(::JvmEnvironmentConfigurator)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )
        useFrontendFacades(::FirFrontendFacade)
        useFrontendHandlers(
            ::FirDiagnosticsHandler,
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
        )

        forTestsMatching("compiler/testData/diagnostics/*") {
            useAfterAnalysisCheckers(
                ::FirIdenticalChecker,
                ::FirFailingTestSuppressor,
            )
            useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
        }

        forTestsMatching("compiler/fir/analysis-tests/testData/*") {
            defaultDirectives {
                +FirDiagnosticsDirectives.FIR_DUMP
            }
        }

        forTestsMatching(
            "compiler/testData/diagnostics/testsWithStdLib/*" or
                    "compiler/fir/analysis-tests/testData/resolveWithStdlib/*" or
                    "compiler/testData/diagnostics/tests/unsignedTypes/*"
        ) {
            defaultDirectives {
                +JvmEnvironmentConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}
