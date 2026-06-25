/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataInMemorySerializationArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.library.SerializedFirMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

internal object JvmMetadataKlibHeaderMergerPhase :
    PipelinePhase<MetadataInMemorySerializationArtifact, MetadataInMemorySerializationArtifact>(name = "JvmMetadataKlibHeaderMergerPhase") {

    override fun executePhase(input: MetadataInMemorySerializationArtifact): MetadataInMemorySerializationArtifact {
        (val metadata, val configuration) = input

        @OptIn(K1Deprecation::class)
        val currentMetadataHeader = parseModuleHeader(metadata.module)
        val previousMetadataHeader = loadPreviousMetadataHeader(configuration)

        val header = KlibMetadataProtoBuf.Header.newBuilder().apply {
            moduleName = currentMetadataHeader.moduleName
            flags = currentMetadataHeader.flags

            addAllPackageFragmentName(buildSet {
                addAll(currentMetadataHeader.packageFragmentNameList)
                addAll(previousMetadataHeader?.packageFragmentNameList ?: emptyList())
            })
        }.build()

        return MetadataInMemorySerializationArtifact(
            SerializedFirMetadata(header.toByteArray(), metadata.fragments, metadata.fragmentNames, metadata.metadataVersion),
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
    private fun loadPreviousMetadataHeader(configuration: CompilerConfiguration): KlibMetadataProtoBuf.Header? {
        val destinationDirectory = configuration.metadataDestinationDirectory?.toPath() ?: return null
        val moduleHeaderFile = KlibMetadataComponentLayout(destinationDirectory).moduleHeaderFile

        if (moduleHeaderFile.notExists()) {
            return null
        }

        @OptIn(K1Deprecation::class)
        return parseModuleHeader(moduleHeaderFile.readBytes())
    }
}
