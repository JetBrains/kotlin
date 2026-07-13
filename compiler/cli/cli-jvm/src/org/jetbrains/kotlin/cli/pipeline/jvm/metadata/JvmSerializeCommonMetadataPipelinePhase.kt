/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataInMemorySerializationArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibInMemorySerializerPhase
import org.jetbrains.kotlin.config.commonFragmentsOutputDir
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.library.SerializedFirMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

internal object JvmSerializeCommonMetadataPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmSerializeCommonMetadataPipelinePhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFrontendPipelineArtifact): JvmFrontendPipelineArtifact {
        serializeFragmentsIfNeeded(input)
        return input
    }

    private fun serializeFragmentsIfNeeded(input: JvmFrontendPipelineArtifact) {
        val configuration = input.configuration
        val outputDir = configuration.commonFragmentsOutputDir ?: return
        val commonFragmentOutputs = input.frontendOutput.outputs.dropLast(1)
        if (commonFragmentOutputs.isEmpty()) return

        for (output in commonFragmentOutputs) {
            val inputForPhase = MetadataFrontendPipelineArtifact(
                AllModulesFrontendOutput(listOf(output)),
                configuration = configuration,
                sourceFiles = input.sourceFiles,
            )

            val destinationDir = outputDir.resolve(output.session.moduleData.name.asStringStripSpecialMarkers())

            val metadataInMemory =
                MetadataKlibInMemorySerializerPhase.executePhase(inputForPhase)
                    .mergeMetadataHeader(loadPreviousMetadataHeader(destinationDir.toPath()))

            JvmMetadataKlibFileWriterPhase.writeToDisc(
                metadataInMemory,
                destinationDir
            )
        }
    }

    private fun MetadataInMemorySerializationArtifact.mergeMetadataHeader(previousMetadataHeader: KlibMetadataProtoBuf.Header?): MetadataInMemorySerializationArtifact {
        if (previousMetadataHeader == null) return this

        (val firMetadata, val configuration) = this
        val currentMetadataHeader = parseModuleHeader(firMetadata.module)

        val header = KlibMetadataProtoBuf.Header.newBuilder().apply {
            moduleName = currentMetadataHeader.moduleName
            flags = currentMetadataHeader.flags

            addAllPackageFragmentName(buildSet {
                addAll(currentMetadataHeader.packageFragmentNameList)
                addAll(previousMetadataHeader.packageFragmentNameList)
            })
        }.build()

        return MetadataInMemorySerializationArtifact(
            SerializedFirMetadata(header.toByteArray(), firMetadata.fragments, firMetadata.fragmentNames, firMetadata.metadataVersion),
            configuration
        )
    }

    /**
     * Loads the module header produced by the previous compilation, or `null` if there is none yet.
     *
     * The header is read directly from the metadata output directory. This is a temporary workaround
     * to unblock testing: the proper incremental flow should report the header output via the file
     * mapping tracker, store it in the incremental cache, and have `IncrementalFirProvider` load the
     * previous header from that cache instead of from the file system.
     *
     * TODO(KT-87249): Handle package removals and incremental tracking. The previous header is currently merged
     *  as-is, so package fragments that are no longer produced by the compilation are kept around even when some
     *  of them are already obsolete. Eventually this file may be removed altogether, see KT-87197.
     */
    private fun loadPreviousMetadataHeader(directory: Path): KlibMetadataProtoBuf.Header? {
        val moduleHeaderFile = KlibMetadataComponentLayout(directory).moduleHeaderFile

        if (moduleHeaderFile.notExists()) {
            return null
        }

        return parseModuleHeader(moduleHeaderFile.readBytes())
    }
}
