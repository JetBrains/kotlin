/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.backend.common.serialization.addLanguageFeaturesToManifest
import org.jetbrains.kotlin.backend.konan.driver.NativePhaseContext
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.konan.config.konanDontCompressKlib
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.writer.includeBitcode
import org.jetbrains.kotlin.konan.library.writer.includeNativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.writer.legacyNativeDependenciesInManifest
import org.jetbrains.kotlin.konan.library.writer.legacyNativeShortNameInManifest
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
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

fun NativePhaseContext.writeKlib(input: KlibWriterInput) {
    val suffix = ".klib"
    val outputPath = input.outputPath
    val dontCompressKlib = config.configuration.konanDontCompressKlib
    val klibOutputFileName = if (!dontCompressKlib) "${outputPath}.klib" else outputPath
    val config = config
    val configuration = config.configuration
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

    if (!dontCompressKlib) {
        if (!klibOutputFileName.endsWith(suffix)) {
            error("please specify correct output: packed: ${!dontCompressKlib}, $klibOutputFileName$suffix")
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
        usedDependenciesFile.writeLines(linkDependencies.map { it.location.absolutePath })
    }

    val nativeTargetsForManifest = config.nativeTargetsForManifest?.map { it.visibleName }
        ?: if (this.config.metadataKlib) {
            // There previously was a workaround in KGP: KGP passed the custom manifest file through `-manifest` CLI
            // parameter to every Kotlin/Native metadata compilation. The manifest file had the empty list of
            // Kotlin/Native targets in the `native_targets` property. As a result, the generated KLIB had no limitations
            // on targets where it can be used.
            //
            // This workaround has been removed in commits aceb4f32dc90e560fb43287c3b38ab40e5f9ca8d and
            // 30d8efaf80e9439259b30fe45e2001eac6957c8f. Still, it is possible to set up a configuration when
            // an older version of KGP is used together with a recent (2.4.+) version of the compiler.
            // In such a configuration there is still a custom manifest file with the `native_targets` property
            // passed to the compiler. But it is forbidden now due to the restrictions that we have in `KlibWriter`:
            // certain sensitive properties such as `native_targets` cannot be set through a custom manifest.
            //
            // To work this around, we remove the `native_targets` property from the custom manifest file if it is empty.
            if (manifestProperties.getProperty(KLIB_PROPERTY_NATIVE_TARGETS)?.isBlank() == true) {
                manifestProperties -= KLIB_PROPERTY_NATIVE_TARGETS
            }

            emptyList()
        } else listOf(target.visibleName)

    KlibWriter {
        format(if (dontCompressKlib) KlibFormat.Directory else KlibFormat.ZipArchive)
        manifest {
            moduleName(libraryName)
            versions(versions)
            platformAndTargets(BuiltInsPlatform.NATIVE, nativeTargetsForManifest)
            customProperties { this += manifestProperties }
            legacyNativeShortNameInManifest(shortLibraryName)
            legacyNativeDependenciesInManifest(linkDependencies.map { it.uniqueName })
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