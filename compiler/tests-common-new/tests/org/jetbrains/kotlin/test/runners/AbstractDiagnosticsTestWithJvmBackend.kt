/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.JvmBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJvmResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.PsiLightTreeMetaInfoProcessor
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractDiagnosticsTestWithJvmBackend<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>> :
    AbstractKotlinCompilerTest() {
    abstract val targetFrontend: FrontendKind<R>
    abstract val frontend: Constructor<FrontendFacade<R>>
    abstract val converter: Constructor<Frontend2BackendConverter<R, I>>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetBackend = TargetBackend.JVM_IR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
        }

        enableMetaInfoHandler()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::ScriptingEnvironmentConfigurator,
        )

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(frontend)
        if (targetFrontend == FrontendKinds.ClassicFrontend) {
            classicFrontendHandlersStep {
                useHandlers(
                    ::ClassicDiagnosticsHandler,
                )
            }
        } else {
            useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
            firHandlersStep {
                useHandlers(::FirDiagnosticsHandler)
            }
        }

        if (targetFrontend == FrontendKinds.ClassicFrontend) {
            useAfterAnalysisCheckers(::FirTestDataConsistencyHandler)
        } else {
            forTestsMatching("compiler/testData/diagnostics/testsWithJvmBackend/*") {
                configurationForClassicAndFirTestsAlongside()
            }
        }

        facadeStep(converter)
        facadeStep(::JvmIrBackendFacade)

        jvmArtifactsHandlersStep {
            useHandlers(
                ::JvmBackendDiagnosticsHandler
            )
        }
    }
}

abstract class AbstractDiagnosticsTestWithJvmIrBackend :
    AbstractDiagnosticsTestWithJvmBackend<ClassicFrontendOutputArtifact, IrBackendInput>() {
    override val targetFrontend: FrontendKind<ClassicFrontendOutputArtifact>
        get() = FrontendKinds.ClassicFrontend

    override val frontend: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter
}

abstract class AbstractFirDiagnosticsTestWithJvmIrBackendBase(
    val parser: FirParser
) : AbstractDiagnosticsTestWithJvmBackend<FirOutputArtifact, IrBackendInput>() {
    override val targetFrontend: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR

    override val frontend: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrJvmResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirParser(parser)
    }
}

abstract class AbstractFirPsiDiagnosticsTestWithJvmIrBackend : AbstractFirDiagnosticsTestWithJvmIrBackendBase(FirParser.Psi)

abstract class AbstractFirLightTreeDiagnosticsTestWithJvmIrBackend : AbstractFirDiagnosticsTestWithJvmIrBackendBase(FirParser.LightTree)
