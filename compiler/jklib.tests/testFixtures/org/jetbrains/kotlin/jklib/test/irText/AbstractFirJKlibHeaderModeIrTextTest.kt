/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerJKlibTest
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirSpecificParserSuppressor
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

@OptIn(TestInfrastructureInternals::class)
abstract class AbstractFirJKlibHeaderModeIrTextTest : AbstractKotlinCompilerJKlibTest() {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetBackend = TargetBackend.JKLIB
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            artifactKind = ArtifactKinds.KLib
            dependencyKind = DependencyKind.Binary
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JKlibSourceRootConfigurator,
            ::JKlibEnvironmentConfigurator,
            ::JKlibJavaSourceConfigurator,
            ::JKlibHeaderModeDependenciesConfigurator,
        )

        useModuleStructureTransformers(SplittingModuleTransformerForJKlibTests())

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useMetaTestConfigurators(::FirSpecificParserSuppressor, ::WithStdlibSkipper, ::WithReflectSkipper)

        facadeStep(::FirCliJKlibFacade)
        firHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        facadeStep(::Fir2IrCliJKlibFacade)
        irHandlersStep {
            commonIrHandlersForCodegenTest()
        }

        facadeStep(::SerializationCliJKlibFacade)
        klibArtifactsHandlersStep()

        facadeStep(::JKlibIrCompilationCliFacade)
        deserializedIrHandlersStep {
            commonIrHandlersForCodegenTest()
        }

        defaultDirectives {
            +CodegenTestDirectives.IGNORE_IR_EXPECT_FLAG
            +JvmEnvironmentConfigurationDirectives.NO_RUNTIME
        }

        useFailureSuppressors(
            ::BlackBoxCodegenSuppressor,
            ::PhasedPipelineChecker.bind(TestPhase.BACKEND)
        )
        enableMetaInfoHandler()
        additionalK2ConfigurationForIrTextTest(FirParser.LightTree)
    }
}
