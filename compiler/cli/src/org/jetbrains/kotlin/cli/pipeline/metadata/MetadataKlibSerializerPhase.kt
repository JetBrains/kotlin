/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.metadata

import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.metadata.buildKotlinMetadataLibrary
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.flatten
import org.jetbrains.kotlin.library.loadSizeInfo
import org.jetbrains.kotlin.library.metadata.KlibMetadataHeaderFlags
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault

object MetadataKlibInMemorySerializerPhase : PipelinePhase<MetadataFrontendPipelineArtifact, MetadataInMemorySerializationArtifact>(
    name = "MetadataKlibInMemorySerializerPhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: MetadataFrontendPipelineArtifact): MetadataInMemorySerializationArtifact {
        val (firResult, configuration, _, _) = input
        val metadataVersion = configuration.klibMetadataVersionOrDefault()
        val fragments = mutableMapOf<String, MutableList<ByteArray>>()

        val analysisResult = firResult.outputs
        for (output in analysisResult) {
            val (session, scopeSession, fir) = output

            val languageVersionSettings = configuration.languageVersionSettings
            for (firFile in fir) {
                val packageFragment = serializeSingleFirFile(
                    firFile,
                    session,
                    scopeSession,
                    actualizedExpectDeclarations = null,
                    FirKLibSerializerExtension(
                        session, scopeSession, session.firProvider, metadataVersion, constValueProvider = null,
                        exportKDoc = false,
                        additionalMetadataProvider = null
                    ),
                    languageVersionSettings,
                )
                fragments.getOrPut(firFile.packageFqName.asString()) { mutableListOf() }.add(packageFragment.toByteArray())
            }
        }

        val header = KlibMetadataProtoBuf.Header.newBuilder()
        header.moduleName = analysisResult.last().session.moduleData.name.asString()

        if (configuration.languageVersionSettings.isPreRelease()) {
            header.flags = KlibMetadataHeaderFlags.PRE_RELEASE
        }

        val fragmentNames = mutableListOf<String>()
        val fragmentParts = mutableListOf<List<ByteArray>>()

        for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
            fragmentNames += fqName
            fragmentParts += fragment
            header.addPackageFragmentName(fqName)
        }

        val module = header.build().toByteArray()
        val serializedMetadata = SerializedMetadata(module, fragmentParts, fragmentNames, metadataVersion.toArray())
        return MetadataInMemorySerializationArtifact(serializedMetadata, configuration)
    }
}

object MetadataKlibFileWriterPhase : PipelinePhase<MetadataInMemorySerializationArtifact, MetadataSerializationArtifact>(
    name = "MetadataKlibFileWriterPhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: MetadataInMemorySerializationArtifact): MetadataSerializationArtifact {
        val destDir = input.configuration.metadataDestinationDirectory!!
        buildKotlinMetadataLibrary(input.configuration, input.metadata, destDir)

        File(destDir.absolutePath).loadSizeInfo()?.flatten()?.let { stats ->
            input.configuration.perfManager?.registerKlibElementStats(stats)
        }

        return MetadataSerializationArtifact(
            outputInfo = null,
            input.configuration,
            destDir.canonicalPath,
        )
    }
}
