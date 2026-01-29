package org.jetbrains.kotlin.test.runners.ir.jklib

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.utils.bind

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.fir.FirSpecificParserSuppressor

abstract class AbstractJKlibIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    val targetFrontend: FrontendKind<FrontendOutput>
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // Custom configuration for JKlib
        // Replicated from commonServicesConfigurationForCodegenAndDebugTest but without ScriptingEnvironmentConfigurator
        globalDefaults {
            frontend = targetFrontend
            targetBackend = TargetBackend.JVM_IR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            artifactKind = ArtifactKinds.KLib
            dependencyKind = DependencyKind.Binary
        }
        
        defaultDirectives {
             // +RUN_DEX_CHECKER // Maybe not needed for KLib
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JKlibSourceRootConfigurator
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useMetaTestConfigurators(::FirSpecificParserSuppressor)

        facadeStep(frontendFacade)
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        facadeStep(frontendToBackendConverter)
        irHandlersStep()

        facadeStep(backendFacade)
        klibArtifactsHandlersStep()

        setupDefaultDirectivesForIrTextTest()
        configureIrHandlersStep {
            setupIrTextDumpHandlers()
        }

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor,
            ::PhasedPipelineChecker.bind(TestPhase.BACKEND)
        )
        enableMetaInfoHandler()
    }
}
