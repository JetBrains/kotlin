/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBlackBoxTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeCodegenTest
import org.jetbrains.kotlin.konan.test.configuration.setupStepsForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunnerGroupingPhase
import org.jetbrains.kotlin.konan.test.klib.NativeCompilerSecondStageFacade
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

abstract class AbstractMyNativeTwoPhaseTest : AbstractTwoStageKotlinCompilerTest() {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        this.extensionContext = context
    }

    override fun configure(builder: TwoPhaseTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
                OPT_IN with listOf(
                    "kotlin.native.internal.InternalForKotlinNative",
                    "kotlin.native.internal.InternalForKotlinNativeTests",
                    "kotlin.experimental.ExperimentalNativeApi"
                )
            }

            commonConfigurationForNativeCodegenTest()

            useMetaTestConfigurators(::DisabledNativeTestSkipper, ::CInteropTestSkipper, ::FileCheckTestSkipper)
            useFailureSuppressors(
                ::FirMetaInfoDiffSuppressor,
                // TODO(KT-84712): Currently the native-specific test suppressor doesn't work correctly in grouping setup
//                ::NativeTestsSuppressor,
            )

            // TODO(KT-84712): should be moved into `AbstractTwoStageKotlinCompilerTest`
            useSourcePreprocessor(::BatchingPackageInserter)

            useAdditionalService { // Register TestRunSettings into TestServices
                extensionContext.createTestRunSettings(extensionContext.computeBlackBoxTestInstances())
            }
            useAdditionalService { // Register TestRunProvider into TestServices
                extensionContext.getOrCreateTestRunProvider()
            }
        }

        nonGroupingPhase {
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )

            // Because of package escaping various dumps for grouping mode would be different from
            // the regular one, so we don't want all the frontend handlers to be set up, only some specific ones.
            setupStepsForNativeFirstStageUpToSerialization(
                includeBasicFirHandlers = false,
                includeDumpFirHandlers = false
            )

            configureFirHandlersStep {
                useHandlers(::FirDiagnosticsHandler, ::NoFirCompilationErrorsHandler)
            }

            configureIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            configureLoweredIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            facadeStep(::KlibSerializerNativeCliFacade)
            klibArtifactsHandlersStep()

            useAdditionalSourceProviders(
                ::NativeLauncherAdditionalSourceProvider,
            )

            forTestsNotMatching(
                "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                        "compiler/testData/diagnostics/*"
            ) {
                defaultDirectives {
                    DIAGNOSTICS with "-warnings"
                }
            }
            enableMetaInfoHandler()
        }

        groupingPhase {
            useConfigurators(::NativeSecondStageEnvironmentConfigurator)

            facadeStep(NativeCompilerSecondStageFacade::Grouping.bind(currentCustomNativeCompilerSettings))
            handlersStep(ArtifactKinds.Native, CompilationStage.SECOND) {
                useHandlers(::NativeBoxRunnerGroupingPhase)
            }
        }
    }
}
