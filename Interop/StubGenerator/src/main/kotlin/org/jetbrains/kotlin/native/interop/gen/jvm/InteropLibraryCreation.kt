/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import kotlinx.metadata.*
import kotlinx.metadata.klib.KlibModuleFragmentWriteStrategy
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.className
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.backend.common.serialization.KlibIrVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.util.*

fun createInteropLibrary(
        metadata: KlibModuleMetadata,
        outputPath: String,
        moduleName: String,
        nativeBitcodeFiles: List<String>,
        target: KonanTarget,
        manifest: Properties,
        dependencies: List<KotlinLibrary>,
        nopack: Boolean,
        shortName: String?,
        staticLibraries: List<String>
) {
    val version = KotlinLibraryVersioning(
            libraryVersion = null,
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = CompilerVersion.CURRENT.toString(),
            metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
            irVersion = KlibIrVersion.INSTANCE.toString()
    )
    val outputPathWithoutExtension = outputPath.removeSuffixIfPresent(".klib")
    KonanLibraryWriterImpl(
            File(outputPathWithoutExtension),
            moduleName,
            version,
            target,

            BuiltInsPlatform.NATIVE,
            nopack = nopack,
            shortName = shortName
    ).apply {
        val metadata = metadata.write(ChunkingWriteStrategy())
        addMetadata(SerializedMetadata(metadata.header, metadata.fragments, metadata.fragmentNames))
        nativeBitcodeFiles.forEach(this::addNativeBitcode)
        addManifestAddend(manifest)
        addLinkDependencies(dependencies)
        staticLibraries.forEach(this::addIncludedBinary)
        commit()
    }
}

// TODO: Consider adding it to kotlinx-metadata-klib.
class ChunkingWriteStrategy(
        private val classesChunkSize: Int = 128,
        private val packagesChunkSize: Int = 128
) : KlibModuleFragmentWriteStrategy {

    override fun processPackageParts(parts: List<KmModuleFragment>): List<KmModuleFragment> {
        if (parts.isEmpty()) return emptyList()
        val fqName = parts.first().fqName
                ?: error("KmModuleFragment should have a not-null fqName!")
        val classFragments = parts.flatMap(KmModuleFragment::classes)
                .chunked(classesChunkSize) { chunk ->
                    KmModuleFragment().also { fragment ->
                        fragment.fqName = fqName
                        fragment.classes += chunk
                        chunk.mapTo(fragment.className, KmClass::name)
                    }
                }
        val packageFragments = parts.mapNotNull(KmModuleFragment::pkg)
                .flatMap { it.functions + it.typeAliases + it.properties }
                .chunked(packagesChunkSize) { chunk ->
                    KmModuleFragment().also { fragment ->
                        fragment.fqName = fqName
                        fragment.pkg = KmPackage().also { pkg ->
                            pkg.fqName = fqName
                            pkg.properties += chunk.filterIsInstance<KmProperty>()
                            pkg.functions += chunk.filterIsInstance<KmFunction>()
                            pkg.typeAliases += chunk.filterIsInstance<KmTypeAlias>()
                        }
                    }
                }
        val result = classFragments + packageFragments
        return if (result.isEmpty()) {
            // We still need to emit empty packages because they may
            // represent parts of package declaration (e.g. platform.[]).
            // Tooling (e.g. `klib contents`) expects this kind of behavior.
            parts
        } else {
            result
        }
    }
}