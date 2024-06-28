/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.js.test.converters.FirJsKlibBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.library.abi.handlers.LibraryAbiDumpHandler
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.NoCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJsResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

/**
 * This test class can potentially be re-used in the future for other backends.
 */
abstract class AbstractLibraryAbiReaderTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    private val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {

    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val converter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>

    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = this@AbstractLibraryAbiReaderTest.frontend
            targetPlatform = this@AbstractLibraryAbiReaderTest.targetPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = this@AbstractLibraryAbiReaderTest.targetBackend
            dependencyKind = DependencyKind.Binary
        }

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor
        )

        applyConfigurators()

        facadeStep(frontendFacade)

        classicFrontendHandlersStep {
            useHandlers(
                ::NoCompilationErrorsHandler
            )
        }

        firHandlersStep {
            useHandlers(
                ::NoFirCompilationErrorsHandler
            )
        }

        facadeStep(converter)
        irHandlersStep()

        facadeStep(backendFacade)
        klibArtifactsHandlersStep {
            useHandlers(
                ::LibraryAbiDumpHandler
            )
        }
    }
}

abstract class AbstractJsLibraryAbiReaderTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> :
    AbstractLibraryAbiReaderTest<FrontendOutput>(
        JsPlatforms.defaultJsPlatform,
        TargetBackend.JS_IR,
    ) {

    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalService(::LibraryProvider)
    }
}

open class AbstractFirJsLibraryAbiReaderTest : AbstractJsLibraryAbiReaderTest<FirOutputArtifact>() {
    final override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    final override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrJsResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirJsKlibBackendFacade

    override fun configure(builder: TestConfigurationBuilder) {
        builder.configureFirParser(FirParser.Psi)
        super.configure(builder)
    }
}

open class AbstractClassicJsLibraryAbiReaderTest : AbstractJsLibraryAbiReaderTest<ClassicFrontendOutputArtifact>() {
    final override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend

    final override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::JsKlibBackendFacade
}
