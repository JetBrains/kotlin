/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LIGHT_TREE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator

abstract class AbstractFirDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
    }
}

abstract class AbstractFirDiagnosticsWithLightTreeTest : AbstractFirDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +USE_LIGHT_TREE
            }
        }
    }
}

// `baseDir` is used in Kotlin plugin from IJ infra
fun TestConfigurationBuilder.baseFirDiagnosticTestConfiguration(
    baseDir: String = ".",
    frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>> = ::FirFrontendFacade
) {
    globalDefaults {
        frontend = FrontendKinds.FIR
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Source
    }

    enableMetaInfoHandler()

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider.bind(baseDir),
        ::CoroutineHelpersSourceFilesProvider.bind(baseDir),
    )

    facadeStep(frontendFacade)
    firHandlersStep {
        useHandlers(
            ::FirDiagnosticsHandler,
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirNoImplicitTypesHandler,
            ::FirScopeDumpHandler,
        )
    }

    useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)

    forTestsMatching("compiler/testData/diagnostics/*") {
        useAfterAnalysisCheckers(
            ::FirIdenticalChecker,
            ::FirFailingTestSuppressor,
        )
        useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/*") {
        defaultDirectives {
            +FIR_DUMP
        }
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/resolve/withAllowedKotlinPackage/*") {
        defaultDirectives {
            +ALLOW_KOTLIN_PACKAGE
        }
    }

    forTestsMatching(
        "compiler/testData/diagnostics/testsWithStdLib/*" or
                "compiler/fir/analysis-tests/testData/resolveWithStdlib/*" or
                "compiler/testData/diagnostics/tests/unsignedTypes/*"
    ) {
        defaultDirectives {
            +WITH_STDLIB
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithExplicitApi/*") {
        defaultDirectives {
            LanguageSettingsDirectives.EXPLICIT_API_MODE with ExplicitApiMode.STRICT
        }
    }

    forTestsMatching(
        "compiler/fir/analysis-tests/testData/resolve/extendedCheckers/*" or
                "compiler/testData/diagnostics/tests/controlFlowAnalysis/deadCode/*"
    ) {
        defaultDirectives {
            +WITH_EXTENDED_CHECKERS
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava17/*") {
        defaultDirectives {
            JDK_KIND with TestJdkKind.FULL_JDK_17
            +WITH_STDLIB
            +WITH_REFLECT
        }
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/resolveWithStdlib/properties/backingField/*") {
        defaultDirectives {
            LANGUAGE + "+ExplicitBackingFields"
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/multiplatform/*") {
        defaultDirectives {
            LANGUAGE + "+MultiPlatformProjects"
        }
    }

    defaultDirectives {
        LANGUAGE + "+EnableDfaWarningsInK2"
    }
}
