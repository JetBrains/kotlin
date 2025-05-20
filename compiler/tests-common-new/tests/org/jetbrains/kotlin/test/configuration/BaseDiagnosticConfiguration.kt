/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DUMP_VFIR
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.TEST_ALONGSIDE_K1_TESTDATA
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EXPLICIT_API_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EXPLICIT_RETURN_TYPES_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.*
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.DuplicateFileNameChecker
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.FixationLogsCollectionConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator
import org.jetbrains.kotlin.test.services.fir.LatestLanguageVersionMetaConfigurator
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

/**
 * General test configuration for FIR-based diagnostic tests
 */
fun TestConfigurationBuilder.configureDiagnosticTest(parser: FirParser) {
    baseFirDiagnosticTestConfiguration()
    enableLazyResolvePhaseChecking()
    configureFirParser(parser)

    useAdditionalService(::LibraryProvider)
}

/**
 * Adds an IR handlers step with diagnostic handler. Used for diagnostic tests which are supposed to report diagnostics
 * from the FIR2IR step
 */
fun TestConfigurationBuilder.configureIrActualizerDiagnosticsTest() {
    irHandlersStep {
        useHandlers(
            ::IrDiagnosticsHandler
        )
    }

    @OptIn(TestInfrastructureInternals::class)
    useModuleStructureTransformers(DuplicateFileNameChecker)
}

/**
 * Setups the configuration for K2 diagnostics tests which share the testdata with K1 diagnostic tests.
 * Enables duplication of testdata in `.fir.kt` files in case if diagnostics are different between K1 and K2
 */
fun TestConfigurationBuilder.configurationForClassicAndFirTestsAlongside(
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::FirTestDataConsistencyHandler,
) {
    defaultDirectives {
        +TEST_ALONGSIDE_K1_TESTDATA
    }
    useAfterAnalysisCheckers(
        ::FirFailingTestSuppressor,
        testDataConsistencyHandler,
    )
    useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
}

/**
 * Setups the base configuration for diagnostic tests
 * Steps:
 * - only FIR frontend step
 * - source dependency kind between modules
 * - target platform is JVM
 *
 * @param [testDataConsistencyHandler] is used to ensure consistency between `.kt` and `.xxx.kt` files if they are present.
 * Known usages:
 * - `.fir.kt` for diagnostics testdata shared between the K1 and K2
 * - `.latestLV.kt` for tests which run with latest LV instead of latest stable LV
 * - `.ll.kt` for AA tests
 * - `.reversed.fir.kt` for reversed AA tests
 */
fun TestConfigurationBuilder.baseFirDiagnosticTestConfiguration(
    @Suppress("unused") baseDir: String = ".",
    frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>> = ::FirFrontendFacade,
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::FirTestDataConsistencyHandler,
) {
    globalDefaults {
        frontend = FrontendKinds.FIR
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Source
    }

    defaultDirectives {
        LANGUAGE + "+EnableDfaWarningsInK2"
    }

    enableMetaInfoHandler()

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
        ::FixationLogsCollectionConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
    )

    facadeStep(frontendFacade)
    firHandlersStep {
        setupHandlersForDiagnosticTest()
    }

    useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
    configureCommonDiagnosticTestPaths(testDataConsistencyHandler)
}

fun HandlersStepBuilder<FirOutputArtifact, FrontendKinds.FIR>.setupHandlersForDiagnosticTest() {
    useHandlers(
        ::FirDiagnosticsHandler,
        ::FirDumpHandler,
        ::FirCfgDumpHandler,
        ::FirVFirDumpHandler,
        ::FirCfgConsistencyHandler,
        ::FirResolvedTypesVerifier,
        ::FirScopeDumpHandler,
        ::FirFixationLogHandler,
    )
}

/**
 * Setups specific directives for tests located (or not located) in some specific directories
 */
fun TestConfigurationBuilder.configureCommonDiagnosticTestPaths(
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::FirTestDataConsistencyHandler,
) {
    forTestsMatching("compiler/testData/diagnostics/*") {
        configurationForClassicAndFirTestsAlongside(testDataConsistencyHandler)
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/*") {
        useAfterAnalysisCheckers(::FirFailingTestSuppressor)
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/resolve/vfir/*") {
        defaultDirectives {
            +DUMP_VFIR
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

    forTestsMatching("compiler/testData/diagnostics/jvmIntegration/*") {
        defaultDirectives {
            +WITH_STDLIB
            +CHECK_COMPILER_OUTPUT
        }
        configurationForClassicAndFirTestsAlongside(testDataConsistencyHandler)
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithExplicitApi/*") {
        defaultDirectives {
            EXPLICIT_API_MODE with ExplicitApiMode.STRICT
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithExplicitReturnTypes/*") {
        defaultDirectives {
            EXPLICIT_RETURN_TYPES_MODE with ExplicitApiMode.STRICT
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/crv/*") {
        defaultDirectives {
            RETURN_VALUE_CHECKER_MODE with ReturnValueCheckerMode.FULL
            +WITH_EXTRA_CHECKERS
            DIAGNOSTICS with "-UNUSED_VARIABLE"
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/crvDisabled/*") {
        defaultDirectives {
            RETURN_VALUE_CHECKER_MODE with ReturnValueCheckerMode.DISABLED
        }
    }

    forTestsMatching(
        "compiler/fir/analysis-tests/testData/resolve/extraCheckers/*" or
                "compiler/testData/diagnostics/tests/controlFlowAnalysis/deadCode/*"
    ) {
        defaultDirectives {
            +WITH_EXTRA_CHECKERS
        }
    }

    forTestsMatching(
        "compiler/fir/analysis-tests/testData/resolve/extraCheckers/*" or
                "compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/bad/returnsImplies/*" or
                "compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/good/returnsImplies/*"
    ) {
        defaultDirectives {
            +WITH_EXPERIMENTAL_CHECKERS
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava17/*") {
        defaultDirectives {
            JDK_KIND with TestJdkKind.FULL_JDK_17
            +WITH_STDLIB
            +WITH_REFLECT
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava21/*") {
        defaultDirectives {
            JDK_KIND with TestJdkKind.FULL_JDK_21
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

    forTestsMatching("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/*") {
        defaultDirectives {
            LANGUAGE + "+NestedTypeAliases"
        }
    }
}

/**
 * Setups running the test with latest LV instead of latest stable LV
 */
fun TestConfigurationBuilder.configurationForTestWithLatestLanguageVersion() {
    defaultDirectives {
        LANGUAGE_VERSION with LanguageVersion.entries.last()
        +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
        +USE_LATEST_LANGUAGE_VERSION
    }
    useMetaTestConfigurators(::LatestLanguageVersionMetaConfigurator)
    useAfterAnalysisCheckers(
        ::FirTestDataConsistencyHandler,
        ::LatestLVIdenticalChecker,
        ::TagsGeneratorChecker
    )
}

/**
 * Enables special handler which ensures that `FirBasedSymbol.lazyResolve()` is called consistently inside the compiler.
 * Note that this handler should be used for regular compiler tests, not AA tests
 */
fun TestConfigurationBuilder.enableLazyResolvePhaseChecking() {
    useAdditionalServices(
        service<FirSessionComponentRegistrar>(::FirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar.coerce())
    )

    // It's important to filter out failures from lazy resolve before calling other suppressors like BlackBoxCodegenSuppressor
    // Otherwise other suppressors can filter out every failure from test and keep it as ignored even if
    // the only problem in lazy resolve contracts, which disables with special directive
    useAfterAnalysisCheckers(::DisableLazyResolveChecksAfterAnalysisChecker, insertAtFirst = true)

    configureFirHandlersStep {
        useHandlers(
            ::FirResolveContractViolationErrorHandler,
        )
    }
}

private class FirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar : FirSessionComponentRegistrar() {
    private val lazyResolver = FirCompilerLazyDeclarationResolverWithPhaseChecking()

    @OptIn(SessionConfiguration::class)
    override fun registerAdditionalComponent(session: FirSession) {
        session.register(FirLazyDeclarationResolver::class, lazyResolver)
    }
}
