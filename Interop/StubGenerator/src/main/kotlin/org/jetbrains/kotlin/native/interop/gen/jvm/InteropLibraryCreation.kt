/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import kotlinx.metadata.klib.KlibModuleMetadata
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
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.util.*

data class LibraryCreationArguments(
        val metadata: KlibModuleMetadata,
        val outputPath: String,
        val moduleName: String,
        val nativeBitcodePath: String,
        val target: KonanTarget,
        val manifest: Properties,
        val dependencies: List<KotlinLibrary>
)

fun createInteropLibrary(arguments: LibraryCreationArguments) {
    val version = KotlinLibraryVersioning(
            libraryVersion = null,
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = CompilerVersion.CURRENT.toString(),
            metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
            irVersion = KlibIrVersion.INSTANCE.toString()
    )
    val outputPathWithoutExtension = arguments.outputPath.removeSuffixIfPresent(".klib")
    KonanLibraryWriterImpl(
            File(outputPathWithoutExtension),
            arguments.moduleName,
            version,
            arguments.target
    ).apply {
        // TODO: Add write strategy that splits big fragments.
        val metadata = arguments.metadata.write()
        addMetadata(SerializedMetadata(metadata.header, metadata.fragments, metadata.fragmentNames))
        addNativeBitcode(arguments.nativeBitcodePath)
        addManifestAddend(arguments.manifest)
        addLinkDependencies(arguments.dependencies)
        commit()
    }
}
