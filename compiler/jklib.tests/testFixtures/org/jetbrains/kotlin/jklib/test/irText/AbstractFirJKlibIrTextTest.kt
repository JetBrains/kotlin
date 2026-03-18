/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText


import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.additionalK2ConfigurationForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirSpecificParserSuppressor
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractFirJKlibIrTextTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JKLIB) {
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
            ::JKlibJavaSourceConfigurator,
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useMetaTestConfigurators(::FirSpecificParserSuppressor, ::WithStdlibSkipper, ::WithReflectSkipper)

        facadeStep(::FirCliJKlibFacade)
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        facadeStep(::Fir2IrCliJKlibFacade)
        irHandlersStep()

        facadeStep(::SerializationCliJKlibFacade)
        klibArtifactsHandlersStep()

        setupDefaultDirectivesForIrTextTest()
        defaultDirectives {
            +CodegenTestDirectives.IGNORE_IR_EXPECT_FLAG
        }
        configureIrHandlersStep {
            setupIrTextDumpHandlers()
        }

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor,
            ::PhasedPipelineChecker.bind(TestPhase.BACKEND)
        )
        enableMetaInfoHandler()
        additionalK2ConfigurationForIrTextTest(FirParser.LightTree)
    }
}

class WithStdlibSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ConfigurationDirectives)

    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.allDirectives.contains(ConfigurationDirectives.WITH_STDLIB)
    }
}

class WithReflectSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JvmEnvironmentConfigurationDirectives)

    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.allDirectives.contains(JvmEnvironmentConfigurationDirectives.WITH_REFLECT)
    }
}
