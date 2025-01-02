/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.NoFir2IrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoLightTreeParsingErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.testTierExceptionInverter
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrConstCheckerHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator
import org.jetbrains.kotlin.test.services.fir.LightTreeSyntaxDiagnosticsReporterHolder
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

fun <A : ResultingArtifact<A>> List<Constructor<AnalysisHandler<A>>>.toTieredHandlersAndCheckerOf(
    tier: TestTierLabel,
): Pair<List<Constructor<AnalysisHandler<A>>>, Constructor<AfterAnalysisChecker>> {
    val invertedHandlers = map { testTierExceptionInverter(tier, it) }
    val checker = { it: TestServices -> TestTierChecker(tier, numberOfMarkerHandlersPerModule = invertedHandlers.size, it) }
    return invertedHandlers to checker
}

fun TestConfigurationBuilder.configureTieredFrontendJvmTest(parser: FirParser) {
    configureDiagnosticTest(parser)

    if (parser == FirParser.LightTree) {
        useAdditionalService { LightTreeSyntaxDiagnosticsReporterHolder() }
    }
}

fun TestConfigurationBuilder.configureTieredFir2IrJvmTest(
    parser: FirParser,
    targetBackend: TargetBackend,
    converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    klibFacades: KlibFacades?,
) {
    // See: compiler/testData/diagnostics/tests/multiplatform/actualAnnotationsNotMatchExpect/checkDiagnosticFullText.kt
    // It expects `+MultiPlatformProjects` to be present a priori because of its location.

    // Also, it's important to configure the same handlers, otherwise differences with the `.fir.kt` files
    // (the absence of diagnostics) would be considered as FIR tier failure.

    configureTieredFrontendJvmTest(parser)

    configureFirHandlersStep {
        useHandlers({ NoFirCompilationErrorsHandler(it, failureDisablesNextSteps = false) })

        // "No Psi parsing handler" is not added as it's the same handler as
        // the one checking "no fir2ir compilation", which is added later.
        // See: compiler/testData/diagnostics/tests/multiplatform/actualAnnotationsNotMatchExpect/valueParameters.kt
        if (parser == FirParser.LightTree) {
            useHandlers(::NoLightTreeParsingErrorsHandler)
        }
    }

    configureAbstractIrTextSettings(targetBackend, converter, klibFacades, includeAllDumpHandlers = false)

    defaultDirectives {
        // In the future we'll probably want to preserve all dumps from lower levels,
        // but for now I'd like to avoid clashing test data files.
//                +FIR_DUMP
        -DUMP_IR
        -DUMP_KT_IR

        // Otherwise, warnings will be suppressed, but we need to render them for FIR tier dumps
        -DIAGNOSTICS
    }

    // Needed for `compiler/testData/diagnostics/tests/modifiers/const/kotlinJavaCycle.kt`.
    configureIrHandlersStep {
        useHandlers(
            ::IrConstCheckerHandler,
        )
    }

    forTestsMatching("compiler/testData/diagnostics/*") {
        // Otherwise, GlobalMetadataInfoHandler may want to write differences to the K1 test data file, not K2
        useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
    }

    forTestsMatching("compiler/testData/ir/irText/properties/backingField/*") {
        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
        }
    }

    // See: `AbstractFirWithActualizerDiagnosticsTest`
    forTestsMatching("diagnostics/tests/multiplatform/*") {
        defaultDirectives {
            +CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS
        }

        @OptIn(TestInfrastructureInternals::class)
        useModuleStructureTransformers(PlatformModuleProvider)

        // It's important to avoid adding `PlatformModuleProvider` when running `testWithJvmBackend` that
        // include `// IGNORE_FIR_DIAGNOSTICS` as then we'll get a new empty module that doesn't emit
        // any errors, but `NoFirCompilationErrorsHandler` will complain.
        // It's probably better to make the diagnostic a module one instead of global and update the
        // test data, but I'm a bit tired of fighting corner cases and don't want to do it now.
        configureIrActualizerDiagnosticsTest()
    }
}

fun TestConfigurationBuilder.configureTieredBackendJvmTest(
    parser: FirParser,
    converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    targetBackend: TargetBackend,
    klibFacades: KlibFacades?,
) {
    configureTieredFir2IrJvmTest(parser, targetBackend, converter, klibFacades)

    configureIrHandlersStep {
        useHandlers(::NoFir2IrCompilationErrorsHandler)
    }

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

    configureJvmBoxCodegenSettings(includeAllDumpHandlers = false)
    configureBlackBoxTestSettings()

    // May be required by some diagnostic tests like:
    // `compiler/testData/diagnostics/testsWithStdLib/multiplatform/actualExternalInJs.kt`.
    // This config matches `forTestsMatching` from `configureTieredFir2IrJvmTest()`
    // to make sure we always set a `LibraryProvider` and don't have duplicates.
    forTestsNotMatching("diagnostics/tests/multiplatform/*") {
        useAdditionalService(::LibraryProvider)
    }
}
