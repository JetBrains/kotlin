/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBlackBoxTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunnerGroupingPhase
import org.jetbrains.kotlin.konan.test.klib.NativeCompilerSecondStageFacade
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.konan.test.suppressors.NativeTestsSuppressor
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.builders.TwoPhaseTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.LibraryProvider
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
            globalDefaults {
                frontend = FrontendKinds.FIR
                targetBackend = TargetBackend.NATIVE
                targetPlatform = NativePlatforms.unspecifiedNativePlatform
                dependencyKind = DependencyKind.Binary
            }

            configureFirParser(FirParser.LightTree)
            useAdditionalService(::LibraryProvider)
            useDirectives(NativeEnvironmentConfigurationDirectives, TestDirectives, LanguageSettingsDirectives)
            useMetaTestConfigurators(::DisabledNativeTestSkipper, ::CInteropTestSkipper, ::FileCheckTestSkipper)
            useAfterAnalysisCheckers(
                ::FirMetaInfoDiffSuppressor,
                ::NativeTestsSuppressor,
            )

            useSourcePreprocessor(::BatchingPackageInserter)

            useAdditionalService { // Register TestRunSettings into TestServices
                extensionContext.createTestRunSettings(extensionContext.computeBlackBoxTestInstances())
            }
            useAdditionalService { // Register TestRunProvider into TestServices
                extensionContext.getOrCreateTestRunProvider()
            }
        }

        nonGroupingPhase {
            useConfigurators(::NativeFirstStageEnvironmentConfigurator)

            commonConfigurationForNativeFirstStageUpToSerialization(
                FrontendKinds.FIR,
                ::FirCliNativeFacade,
                ::Fir2IrCliNativeFacade,
                ::NativePreSerializationLoweringCliFacade,
            )

            // 1st stage (sources -> klibs)
            useAdditionalSourceProviders(
                ::NativeLauncherAdditionalSourceProvider,
            )

            facadeStep(::KlibSerializerNativeCliFacade)
            klibArtifactsHandlersStep()

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
