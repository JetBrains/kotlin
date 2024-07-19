/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SIGNATURES
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LINK_VIA_SIGNATURES_K1
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    private val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val converter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>

    data class KlibFacades(
        val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
        val deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>,
    )

    /**
     * Facades for serialization and deserialization to/from klibs.
     */
    open val klibFacades: KlibFacades?
        get() = null

    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = this@AbstractIrTextTest.frontend
            targetPlatform = this@AbstractIrTextTest.targetPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = this@AbstractIrTextTest.targetBackend
            dependencyKind = when (targetBackend) {
                TargetBackend.JS_IR, TargetBackend.WASM -> DependencyKind.KLib // these irText pipelines register Klib artifacts during *KlibSerializerFacade
                else -> DependencyKind.Source
            }
        }

        defaultDirectives {
            +DUMP_IR
            +DUMP_KT_IR
            +DUMP_SIGNATURES
            +LINK_VIA_SIGNATURES_K1
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            DIAGNOSTICS with "-warnings"
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
                ::NoCompilationErrorsHandler,
                ::ClassicDiagnosticsHandler
            )
        }

        firHandlersStep {
            useHandlers(
                ::NoFirCompilationErrorsHandler,
                ::FirDiagnosticsHandler
            )
        }

        facadeStep(converter)

        irHandlersStep { useIrTextHandlers(this@configuration, isDeserializedInput = false) }

        klibFacades?.let { klibSteps(it) }
    }

    private fun TestConfigurationBuilder.klibSteps(klibFacades: KlibFacades) = klibFacades.run {
        facadeStep(serializerFacade)
        klibArtifactsHandlersStep()
        facadeStep(deserializerFacade)
        deserializedIrHandlersStep { useIrTextHandlers(this@klibSteps, isDeserializedInput = true) }
    }

    private fun <InputArtifactKind> HandlersStepBuilder<IrBackendInput, InputArtifactKind>.useIrTextHandlers(
        testConfigurationBuilder: TestConfigurationBuilder,
        isDeserializedInput: Boolean,
    ) where InputArtifactKind : BackendKind<IrBackendInput> {
        useHandlers(
            ::IrTextDumpHandler.bind(isDeserializedInput),
            ::IrTreeVerifierHandler,
            ::IrPrettyKotlinDumpHandler,
            ::IrSourceRangesDumpHandler,
            ::IrMangledNameAndSignatureDumpHandler,
        )
        testConfigurationBuilder.useAfterAnalysisCheckers(
            ::FirIrDumpIdenticalChecker,
        )
    }

    protected fun TestConfigurationBuilder.commonConfigurationForK2(parser: FirParser) {
        configureFirParser(parser)

        configureFirHandlersStep {
            useHandlersAtFirst(
                ::FirDumpHandler,
                ::FirScopeDumpHandler,
            )
        }

        forTestsMatching("compiler/testData/ir/irText/properties/backingField/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}

