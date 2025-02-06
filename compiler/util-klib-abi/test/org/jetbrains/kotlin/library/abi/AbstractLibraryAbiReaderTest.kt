/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.js.test.converters.FirJsKlibSerializerFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibSerializerFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.NoCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.DUMP_KLIB_ABI
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KlibAbiDumpMode
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
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

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = this@AbstractLibraryAbiReaderTest.frontend
            targetPlatform = this@AbstractLibraryAbiReaderTest.targetPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = this@AbstractLibraryAbiReaderTest.targetBackend
            dependencyKind = DependencyKind.Binary
        }
        defaultDirectives {
            DUMP_KLIB_ABI with KlibAbiDumpMode.ALL_SIGNATURE_VERSIONS
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
        useAdditionalService(::LibraryProvider)

        facadeStep(frontendFacade)
        classicFrontendHandlersStep {
            useHandlers(::NoCompilationErrorsHandler)
        }
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        facadeStep(converter)
        irHandlersStep()

        facadeStep(backendFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }
    }
}

abstract class AbstractJsLibraryAbiReaderTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> :
    AbstractLibraryAbiReaderTest<FrontendOutput>(JsPlatforms.defaultJsPlatform, TargetBackend.JS_IR) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )
        super.configure(builder)
    }
}

open class AbstractFirJsLibraryAbiReaderTest : AbstractJsLibraryAbiReaderTest<FirOutputArtifact>() {
    final override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    final override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirJsKlibSerializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LANGUAGE with "+ContextReceivers"
        }
        configureFirParser(FirParser.LightTree)
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
        get() = ::JsKlibSerializerFacade
}
