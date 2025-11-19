/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder

fun PhaseContext.firSerializer(input: FirOutput): SerializerOutput? = when (input) {
    !is FirOutput.Full -> null
    else -> firSerializerBase(input.firResult, null)
}

fun PhaseContext.fir2IrSerializer(input: FirSerializerInput): SerializerOutput {
    return firSerializerBase(input.firToIrOutput.frontendOutput, input.firToIrOutput, produceHeaderKlib = input.produceHeaderKlib)
}

private fun PhaseContext.firSerializerBase(
        firResult: AllModulesFrontendOutput,
        fir2IrOutput: Fir2IrOutput?,
        produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val configuration = config.configuration
    val usedResolvedLibraries = fir2IrOutput?.let {
        config.resolvedLibraries.getFullResolvedList(TopologicalLibraryOrder).filter {
            (!it.isDefault && !configuration.getBoolean(KonanConfigKeys.Companion.PURGE_USER_LIBS)) || it in fir2IrOutput.usedLibraries
        }
    }

    val irModuleFragment = fir2IrOutput?.fir2irActualizedResult?.irModuleFragment
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter.deduplicating(), configuration.languageVersionSettings)
    val serializerOutput = serializeModuleIntoKlib(
            moduleName = irModuleFragment?.name?.asString() ?: firResult.outputs.last().session.moduleData.name.asString(),
            irModuleFragment = irModuleFragment,
            configuration = configuration,
            diagnosticReporter = irDiagnosticReporter,
            metadataSerializer = Fir2KlibMetadataSerializer(
                configuration,
                firResult.outputs,
                fir2IrOutput?.fir2irActualizedResult,
                exportKDoc = config.configuration.getBoolean(KonanConfigKeys.Companion.EXPORT_KDOC),
                produceHeaderKlib = produceHeaderKlib,
            ),
            cleanFiles = emptyList(),
            dependencies = usedResolvedLibraries?.map { it.library }.orEmpty(),
            createModuleSerializer = { irDiagnosticReporter ->
                KonanIrModuleSerializer(
                    settings = IrSerializationSettings(
                        configuration = configuration,
                        publicAbiOnly = produceHeaderKlib,
                    ),
                    diagnosticReporter = irDiagnosticReporter,
                    irBuiltIns = fir2IrOutput?.fir2irActualizedResult?.irBuiltIns!!,
                )
            },
    )
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    diagnosticReporter.reportToMessageCollector(messageCollector, renderDiagnosticNames)
    if (diagnosticReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were errors during module serialization")
    }
    return serializerOutput
}
