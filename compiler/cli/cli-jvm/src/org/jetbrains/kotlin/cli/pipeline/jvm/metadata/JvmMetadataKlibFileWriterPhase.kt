/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataInMemorySerializationArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataSerializationArtifact
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedFirMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loadSizeInfo
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.util.metadataVersion
import java.io.File
import kotlin.io.path.absolute

internal object JvmMetadataKlibFileWriterPhase : PipelinePhase<MetadataInMemorySerializationArtifact, MetadataSerializationArtifact>(
    name = "JvmMetadataKlibFileWriterPhase",
    preActions = setOf(),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: MetadataInMemorySerializationArtifact): MetadataSerializationArtifact {
        val destDir = input.configuration.metadataDestinationDirectory!!
        writeToDisc(input, destDir)

        return MetadataSerializationArtifact(
            outputInfo = null,
            input.configuration,
            destDir.canonicalPath,
        )
    }

    fun writeToDisc(input: MetadataInMemorySerializationArtifact, destDir: File) {
        buildFirMetadataLibrary(input.configuration, input.firMetadata, destDir)

        loadSizeInfo(destDir.toPath().absolute())?.flatten()?.let { stats ->
            input.configuration.perfManager?.registerKlibElementStats(stats)
        }
    }

    private fun buildFirMetadataLibrary(
        configuration: CompilerConfiguration,
        serializedMetadata: SerializedFirMetadata,
        destDir: File,
    ) {
        val versions = KotlinLibraryVersioning(
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KotlinCompilerVersion.getVersion(),
            metadataVersion = configuration.metadataVersion(),
        )

        KlibWriter {
            manifest {
                moduleName(configuration[CommonConfigurationKeys.MODULE_NAME]!!)
                versions(versions)
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
            allowIncrementalOverwriting(configuration.incrementalCompilation)
            include(JvmMetadataComponentWriter(serializedMetadata, configuration.fileMappingTracker))
        }.writeTo(destDir.absolutePath)
    }
}
