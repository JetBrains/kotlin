/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.deserializedIrHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractK2CompatibilityTest<FrontendOutput, BackendOutput>(
    private val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) where FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>, BackendOutput : ResultingArtifact.Binary<BackendOutput> {
    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val frontendToBackend: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BackendOutput>>
    abstract val deserializedLazyIrFacade: Constructor<AbstractTestFacade<BackendOutput, IrBackendInput>>

    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = this@AbstractK2CompatibilityTest.frontend
            targetPlatform = this@AbstractK2CompatibilityTest.targetPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = this@AbstractK2CompatibilityTest.targetBackend
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +CodegenTestDirectives.DUMP_SIGNATURES
            +LanguageSettingsDirectives.LINK_VIA_SIGNATURES
            DiagnosticsDirectives.DIAGNOSTICS with "-warnings"
        }

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor
        )

        applyConfigurators()
        enableMetaInfoHandler()

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::CodegenHelpersSourceFilesProvider,
        )

        facadeStep(frontendFacade)

        classicFrontendHandlersStep {
            useHandlers(
                ::NoCompilationErrorsHandler, ::ClassicDiagnosticsHandler
            )
        }

        firHandlersStep {
            useHandlers(
                ::NoFirCompilationErrorsHandler, ::FirDiagnosticsHandler
            )
        }

        facadeStep(frontendToBackend)
        facadeStep(backendFacade)
        facadeStep(deserializedLazyIrFacade)

        deserializedIrHandlersStep {
            useHandlers(
                ::IrSignatureDumpHandler,
            )
        }
    }

    protected fun TestConfigurationBuilder.commonConfigurationForK2(parser: FirParser) {
        configureFirParser(parser)
        useAfterAnalysisCheckers(
            ::FirIrDumpIdenticalChecker,
        )

        forTestsMatching("compiler/testData/ir/irText/properties/backingField/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}

abstract class AbstractClassicToK2CompatibilityTest<BackendOutput : ResultingArtifact.Binary<BackendOutput>>(
    targetPlatform: TargetPlatform,
    targetBackend: TargetBackend,
) : AbstractK2CompatibilityTest<ClassicFrontendOutputArtifact, BackendOutput>(targetPlatform, targetBackend) {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend

    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    abstract override val frontendToBackend: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
}

abstract class AbstractFirToK2CompatibilityTest<BackendOutput : ResultingArtifact.Binary<BackendOutput>>(
    targetPlatform: TargetPlatform,
    targetBackend: TargetBackend,
    private val parser: FirParser,
) : AbstractK2CompatibilityTest<FirOutputArtifact, BackendOutput>(targetPlatform, targetBackend) {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    abstract override val frontendToBackend: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonConfigurationForK2(parser)
    }
}
