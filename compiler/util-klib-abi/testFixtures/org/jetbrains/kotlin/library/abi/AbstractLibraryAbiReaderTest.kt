/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
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
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.DUMP_KLIB_ABI
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KlibAbiDumpMode
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator

/**
 * This test class can potentially be re-used in the future for other backends.
 */
abstract class AbstractLibraryAbiReaderTest(
    private val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
    abstract val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
    abstract val preserializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureFirParser(FirParser.LightTree)
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = this@AbstractLibraryAbiReaderTest.targetPlatform
            artifactKind = ArtifactKind.NoArtifact
            targetBackend = this@AbstractLibraryAbiReaderTest.targetBackend
            dependencyKind = DependencyKind.Binary
        }
        defaultDirectives {
            DUMP_KLIB_ABI with KlibAbiDumpMode.ALL_SIGNATURE_VERSIONS
            LANGUAGE with listOf(
                "-${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "-${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
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

        facadeStep(preserializerFacade)
        loweredIrHandlersStep { useHandlers(::IrDiagnosticsHandler) }

        facadeStep(backendFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }
    }
}

abstract class AbstractJsLibraryAbiReaderTest : AbstractLibraryAbiReaderTest(JsPlatforms.defaultJsPlatform, TargetBackend.JS_IR) {
    final override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliWebFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliWebFacade

    override val preserializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::JsIrPreSerializationLoweringFacade

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirKlibSerializerCliWebFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsFirstStageEnvironmentConfigurator,
        )
        super.configure(builder)
    }
}

open class AbstractJsLibraryAbiReaderWithInlinedFunInKlibTest : AbstractJsLibraryAbiReaderTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }
        super.configure(builder)
    }
}
