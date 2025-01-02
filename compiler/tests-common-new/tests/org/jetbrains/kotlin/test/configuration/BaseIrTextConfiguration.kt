/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.FirIrDumpIdenticalChecker
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrSourceRangesDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTreeVerifierHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.SerializedIrDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.deserializedIrHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.DUMP_KLIB_ABI
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KlibAbiDumpMode
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LINK_VIA_SIGNATURES_K1
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider

/**
 * Adds IR text handlers to the IR handlers step
 */
fun <InputArtifactKind> HandlersStepBuilder<IrBackendInput, InputArtifactKind>.useIrTextHandlers(
    testConfigurationBuilder: TestConfigurationBuilder,
    includeAllDumpHandlers: Boolean = true,
) where InputArtifactKind : BackendKind<IrBackendInput> {
    useHandlers(
        ::IrTextDumpHandler,
        ::IrTreeVerifierHandler,
        ::IrPrettyKotlinDumpHandler,
    )
    if (includeAllDumpHandlers) {
        useHandlers(
            ::IrSourceRangesDumpHandler,
        )
    }
    testConfigurationBuilder.useAfterAnalysisCheckers(
        ::FirIrDumpIdenticalChecker,
    )
}

/**
 * General configuration for all IR text tests for all backends
 * Steps (JVM):
 * - FIR frontend
 * - FIR2IR
 *
 * Steps (non-JVM):
 * - FIR frontend
 * - FIR2IR
 * - KLib serializer
 * - KLib deserializer
 *
 * IR text handlers are set up for:
 * - IR produced by fir2ir
 * - IR deserialized from KLibs (if present)
 */
fun <FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> TestConfigurationBuilder.configureAbstractIrTextSettings(
    targetBackend: TargetBackend,
    converter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>,
    klibFacades: KlibFacades?,
    /**
     * When tiered tests are implemented, runners of later tiers may be run for test data
     * originally designed for lower tiers, but sometimes handlers interfere with one another.
     * Until this is fixed, tiered runners will need a workaround.
     * See: KT-67281.
     */
    includeAllDumpHandlers: Boolean,
) {
    globalDefaults {
        artifactKind = ArtifactKind.NoArtifact
        this.targetBackend = targetBackend
        dependencyKind = when (targetBackend) {
            TargetBackend.JS_IR, TargetBackend.WASM -> DependencyKind.KLib // these irText pipelines register Klib artifacts during *KlibSerializerFacade
            else -> DependencyKind.Source
        }
    }

    defaultDirectives {
        +DUMP_IR
        +DUMP_KT_IR
        +LINK_VIA_SIGNATURES_K1
        +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        DIAGNOSTICS with "-warnings"
        DUMP_KLIB_ABI with KlibAbiDumpMode.DEFAULT
    }

    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor
    )

    enableMetaInfoHandler()

    useAdditionalSourceProviders(
        ::CodegenHelpersSourceFilesProvider,
    )

    facadeStep(converter)

    irHandlersStep { useIrTextHandlers(this@configureAbstractIrTextSettings, includeAllDumpHandlers) }

    if (klibFacades != null) {
        irHandlersStep {
            useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) })
        }

        facadeStep(klibFacades.serializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }
        facadeStep(klibFacades.deserializerFacade)

        deserializedIrHandlersStep {
            useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) })
        }
    }
}
