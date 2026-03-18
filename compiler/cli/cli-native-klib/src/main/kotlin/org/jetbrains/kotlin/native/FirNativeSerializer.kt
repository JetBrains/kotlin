/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.config.konanExportKdoc
import org.jetbrains.kotlin.konan.config.konanPurgeUserLibs
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution

internal fun NativeFirstStagePhaseContext.firSerializerBase(
        configuration: CompilerConfiguration,
        firResult: AllModulesFrontendOutput,
        fir2IrOutput: Fir2IrOutput?,
        produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val usedLibraries = fir2IrOutput?.let {
        config.loadedKlibs.all.filter { library ->
            // TODO (KT-60874): Need to clarify why used-specified libraries are forced to stay in the list of "used libraries"
            //  even when they are not used. Note that the default value for `konanPurgeUserLibs` is always `false`.
            val forceLibraryToStayInUsedLibrariesList = !configuration.konanPurgeUserLibs && !library.isFromKotlinNativeDistribution

            forceLibraryToStayInUsedLibrariesList || library in fir2IrOutput.usedLibraries
        }
    }

    val irModuleFragment = fir2IrOutput?.fir2irActualizedResult?.irModuleFragment
    val diagnosticReporter = configuration.diagnosticsCollector
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, configuration.languageVersionSettings)
    return serializeModuleIntoKlib(
            moduleName = irModuleFragment?.name?.asString() ?: firResult.outputs.last().session.moduleData.name.asString(),
            irModuleFragment = irModuleFragment,
            configuration = configuration,
            diagnosticReporter = irDiagnosticReporter,
            metadataSerializer = Fir2KlibMetadataSerializer(
                configuration,
                firResult.outputs,
                fir2IrOutput?.fir2irActualizedResult,
                exportKDoc = config.configuration.konanExportKdoc,
                produceHeaderKlib = produceHeaderKlib,
            ),
            cleanFiles = emptyList(),
            dependencies = usedLibraries.orEmpty(),
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
}
