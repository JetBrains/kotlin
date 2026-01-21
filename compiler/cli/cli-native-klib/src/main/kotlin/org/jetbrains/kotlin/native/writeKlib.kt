/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.backend.common.serialization.addLanguageFeaturesToManifest
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.writer.includeBitcode
import org.jetbrains.kotlin.konan.library.writer.includeNativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.writer.legacyNativeDependenciesInManifest
import org.jetbrains.kotlin.konan.library.writer.legacyNativeShortNameInManifest
import org.jetbrains.kotlin.library.KLIB_PROPERTY_HEADER
import org.jetbrains.kotlin.library.KlibFormat
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loadSizeInfo
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import java.util.Properties

fun PhaseContext.writeKlib(input: KlibWriterInput, klibOutputFileName: String, suffix: String) {
    val config = config
    val configuration = config.configuration
    val nopack = configuration.getBoolean(KonanConfigKeys.NOPACK)
    val libraryName = config.moduleId
    val shortLibraryName = config.shortModuleName
    val versions = KotlinLibraryVersioning(
        compilerVersion = KotlinCompilerVersion.getVersion().toString(),
        abiVersion = configuration.klibAbiVersionForManifest(),
        metadataVersion = configuration.klibMetadataVersionOrDefault(),
    )
    val target = config.target
    val manifestProperties = config.manifestProperties ?: Properties()

    if (input.produceHeaderKlib) {
        manifestProperties.setProperty(KLIB_PROPERTY_HEADER, "true")
    }

    addLanguageFeaturesToManifest(manifestProperties, configuration.languageVersionSettings)

    val nativeTargetsForManifest = config.nativeTargetsForManifest?.map { it.visibleName } ?: listOf(target.visibleName)

    if (!nopack) {
        if (!klibOutputFileName.endsWith(suffix)) {
            error("please specify correct output: packed: ${!nopack}, $klibOutputFileName$suffix")
        }
    }

    /*
    metadata libraries do not have 'link' dependencies, as there are several reasons
    why a consumer might not be able to provide the same compile classpath as the producer
    (e.g. commonized cinterops, host vs client environment differences).
    */
    val linkDependencies = if (this.config.metadataKlib) emptyList()
    else input.serializerOutput.neededLibraries

    config.writeDependenciesOfProducedKlibTo?.let { path ->
        val usedDependenciesFile = File(path)
        // We write out the absolute path instead of canonical here to avoid resolving symbolic links
        // as that can make it difficult to map the dependencies back to the command line arguments.
        usedDependenciesFile.writeLines(linkDependencies.map { it.libraryFile.absolutePath })
    }

    KlibWriter {
        format(if (nopack) KlibFormat.Directory else KlibFormat.ZipArchive)
        manifest {
            moduleName(libraryName)
            versions(versions)
            platformAndTargets(BuiltInsPlatform.NATIVE, nativeTargetsForManifest)
            legacyNativeShortNameInManifest(shortLibraryName)
            legacyNativeDependenciesInManifest(linkDependencies.map { it.uniqueName })
            customProperties { this += manifestProperties }
        }
        includeMetadata(input.serializerOutput.serializedMetadata!!)
        includeIr(input.serializerOutput.serializedIr)
        includeBitcode(target, config.nativeLibraries)
        includeNativeIncludedBinaries(target, config.includeBinaries)
    }.writeTo(klibOutputFileName)

    loadSizeInfo(File(klibOutputFileName))?.flatten()?.let { stats ->
        performanceManager?.registerKlibElementStats(stats)
    }
}