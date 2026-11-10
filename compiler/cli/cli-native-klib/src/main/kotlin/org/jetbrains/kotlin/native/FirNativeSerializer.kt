/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.ProcessFun
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.common.serialization.getSerializedData
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.parseTreeKind
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalDataProvider
import org.jetbrains.kotlin.config.incrementalResultsConsumer
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.library.isExplicitlySpecifiedByUserInCLIArgument
import org.jetbrains.kotlin.library.SerializedIrFile
import java.io.File

internal fun NativeFirstStagePhaseContext.firSerializerBase(
    configuration: CompilerConfiguration,
    firResult: AllModulesFrontendOutput,
    fir2IrOutput: Fir2IrOutput?,
    produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val usedLibraries = fir2IrOutput?.let {
        config.loadedKlibs.all.filter { library ->
            if (library.isExplicitlySpecifiedByUserInCLIArgument) {
                // This is the dependency explicitly specified by the user in one of the compiler's CLI arguments: -library, -Xinclude.
                //
                // We assume such a library as "used" even if we cannot immediately prove there are declarations belonging to it
                // that are used in other dependencies or the current module. It might happen, such declarations exist but are
                // used only in bodies of functions in other dependencies, which cannot be seen during the frontend phase.
                //
                // So, we agree to always include such libraries in the list of "used libraries".
                true
            } else if (library in fir2IrOutput.usedLibraries) {
                // This dependency indeed contains some declarations that are used in other dependencies or the current module.
                true
            } else false
        }
    }

    val irModuleFragment = fir2IrOutput?.fir2irActualizedResult?.irModuleFragment
    val diagnosticReporter = configuration.diagnosticsCollector
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        diagnosticReporter,
        configuration.languageVersionSettings,
        configuration.parseTreeKind,
    )
    val fir2KlibMetadataSerializer = Fir2KlibMetadataSerializer(
        configuration,
        firResult.outputs,
        fir2IrOutput?.fir2irActualizedResult,
        produceHeaderKlib = produceHeaderKlib,
    )
    return serializeModuleIntoKlib(
        moduleName = irModuleFragment?.name?.asString() ?: firResult.outputs.last().session.moduleData.name.asString(),
        irModuleFragment = irModuleFragment,
        configuration = configuration,
        diagnosticReporter = irDiagnosticReporter,
        metadataSerializer = fir2KlibMetadataSerializer,
        cleanFiles = configuration.incrementalDataProvider?.getSerializedData(fir2KlibMetadataSerializer.sourceFiles) ?: emptyList(),
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
        processCompiledFileData = configuration.incrementalResultsConsumer?.let { icConsumer ->
            fun SerializedIrFile.processIrFile(ioFile: File, process: ProcessFun) {
                process(
                    ioFile,
                    fileData,
                    types,
                    signatures,
                    strings,
                    declarations,
                    bodies,
                    fqName.toByteArray(),
                    debugInfo,
                    fileEntries,
                )
            }

            { ioFile, compiledFile, inlineIds ->
                icConsumer.processPackagePart(ioFile, compiledFile.metadata)
                compiledFile.irData!!.processIrFile(ioFile, icConsumer::processIrFile)
                compiledFile.irInlineData?.processIrFile(ioFile, icConsumer::processIrInlineFile)
                icConsumer.processIrInlineIds(ioFile, inlineIds)
                compiledFile.signatureIndex?.let { icConsumer.processSignatureIndex(ioFile, it) }
            }
        },
    )
}
