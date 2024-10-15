/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.backend.handlers.NoBackendCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrConstCheckerHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.TestTierChecker
import org.jetbrains.kotlin.test.runners.TestTiers
import org.jetbrains.kotlin.test.runners.ir.AbstractIrTextTest.KlibFacades
import org.jetbrains.kotlin.test.runners.ir.configureTieredFir2IrJvmTest
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

abstract class AbstractFirBlackBoxCodegenTestBase(
    val parser: FirParser
) : AbstractJvmBlackBoxCodegenTestBase<FirOutputArtifact>(FrontendKinds.FIR) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureFirParser(parser)

            configureFirHandlersStep {
                useHandlersAtFirst(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirResolvedTypesVerifier,
                )

                useHandlersAtFirst(
                    ::FirScopeDumpHandler,
                )
            }

            configureIrHandlersStep {
                useHandlers(
                    ::IrDiagnosticsHandler,
                )
            }

            configureIrHandlersStep {
                useHandlers(
                    ::IrConstCheckerHandler
                )
            }

            configureBlackBoxTestSettings()
            configureDumpHandlersForCodegenTest()
        }
    }
}

fun TestConfigurationBuilder.configureBlackBoxTestSettings() {
    defaultDirectives {
        // See KT-44152
        -USE_PSI_CLASS_FILES_READING
    }

    useAfterAnalysisCheckers(
        ::FirMetaInfoDiffSuppressor
    )

    baseFirBlackBoxCodegenTestDirectivesConfiguration()
}

fun TestConfigurationBuilder.baseFirBlackBoxCodegenTestDirectivesConfiguration() {
    forTestsMatching("*WithStdLib/*") {
        defaultDirectives {
            +WITH_STDLIB
        }
    }

    forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
        }
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBlackBoxCodegenTest : AbstractFirBlackBoxCodegenTestBase(FirParser.Psi)

fun TestConfigurationBuilder.configureTieredBackendJvmTest(
    parser: FirParser,
    testInfo: KotlinTestInfo,
    converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    targetBackend: TargetBackend,
    klibFacades: KlibFacades?,
    enableBoxHandler: Boolean,
) {
    configureTieredFir2IrJvmTest(parser, testInfo, targetBackend, converter, klibFacades)

    globalDefaults {
        // FIR2IR sets it to `NoArtifact`, but this prevents BACKEND and BOX steps
        artifactKind = null
    }

    // See: org.jetbrains.kotlin.test.runners.codegen.commonServicesConfigurationForCodegenTest
    commonServicesMinimalSettingsConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)
    useAdditionalSourceProviders(
        ::MainFunctionForBlackBoxTestsSourceProvider
    )

    facadeStep(::JvmIrBackendFacade)
    jvmArtifactsHandlersStep(init = {})

    configureJvmArtifactsHandlersStep {
        commonBackendHandlersForCodegenTest()
    }

    // Tests in `testsWithJvmBackend` run the backend and compare diagnostics dumps,
    // But no differences in dumps doesn't mean it's safe to run BOX.
    configureJvmArtifactsHandlersStep {
        useHandlers(::NoBackendCompilationErrorsHandler)
    }

    configureJvmArtifactsHandlersStep {
        if (enableBoxHandler) {
            useHandlers(::JvmBoxRunner)
        }
    }

    configureJvmBoxCodegenSettings(includeAllDumpHandlers = false)
    configureBlackBoxTestSettings()

    // May be required by some diagnostic tests like:
    // `compiler/testData/diagnostics/testsWithStdLib/multiplatform/actualExternalInJs.kt`.
    // This config matches `forTestsMatching` from `configureTieredFir2IrJvmTest()`
    // to make sure we always set a `LibraryProvider` and don't have duplicates.
    forTestsNotMatching("diagnostics/tests/multiplatform/*") {
        useAdditionalService(::LibraryProvider)
    }

    /**
     * For example, see `compiler/testData/diagnostics/testsWithStdLib/multiplatform/duplicateSupertype.kt`
     * which yields `CONFLICTING_INHERITED_JVM_DECLARATIONS` in JVM backend.
     */
    forTestsMatching(
        "compiler/testData/diagnostics/tests/*" or
                "compiler/testData/diagnostics/testsWithStdLib/*"
    ) {
        defaultDirectives {
            +IGNORE_BACKEND_DIAGNOSTICS
        }
    }
}

abstract class AbstractTieredBackendJvmTest(
    private val supportedTier: TestTiers,
    parser: FirParser,
) : AbstractFirBlackBoxCodegenTestBase(parser) {
    open val enableBoxHandler get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        builder.configureTieredBackendJvmTest(
            parser, testInfo, frontendToBackendConverter, targetBackend,
            klibFacades = null,
            enableBoxHandler = enableBoxHandler,
        )

        builder.useAfterAnalysisCheckers(
            { TestTierChecker(supportedTier, it) },
        )
    }
}

open class AbstractTieredBackendJvmLightTreeTest : AbstractTieredBackendJvmTest(TestTiers.BACKEND, FirParser.LightTree)

abstract class AbstractTieredBoxJvmTest(parser: FirParser) : AbstractTieredBackendJvmTest(TestTiers.BOX, parser) {
    override val enableBoxHandler get() = true
}

open class AbstractTieredBoxJvmLightTreeTest : AbstractTieredBoxJvmTest(FirParser.LightTree)
