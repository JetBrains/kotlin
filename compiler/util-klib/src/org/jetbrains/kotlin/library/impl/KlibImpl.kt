/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KlibAttributes
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibConstants.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KlibLayoutReaderFactory
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.builtInsPlatform
import org.jetbrains.kotlin.library.loader.KlibManifestTransformer
import org.jetbrains.kotlin.library.readKonanLibraryVersioning
import org.jetbrains.kotlin.konan.file.File as KlibFile

internal class KlibImpl(
    override val location: KlibFile,
    zipFileSystemAccessor: ZipFileSystemAccessor,
    manifestTransformer: KlibManifestTransformer?,
) : KotlinLibrary {

    private val components: KlibComponentsCache
    override val manifestProperties: Properties

    init {
        val layoutReaderFactory = KlibLayoutReaderFactory(
            klibFile = location,
            zipFileSystemAccessor = zipFileSystemAccessor
        )

        // Note: readInPlace() will fail in case there is no manifest file or the file is malformed.
        manifestProperties = layoutReaderFactory.createLayoutReader<KlibManifestComponentLayout>(::KlibManifestComponentLayout)
            .readInPlace { layout -> layout.manifestFile.loadProperties() }
            .let { properties -> manifestTransformer?.transform(properties) ?: properties }

        components = KlibComponentsCache(layoutReaderFactory)
    }

    override val versions: KotlinLibraryVersioning = manifestProperties.readKonanLibraryVersioning()

    override fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>) = components.getComponent(kind)

    override val attributes = KlibAttributes()

    override val libraryName get() = location.path
    override val libraryFile get() = location

    override fun toString() = listOfNotNull(
        location.path,
        versions.abiVersion?.let { "$KLIB_PROPERTY_ABI_VERSION=$it" },
        versions.metadataVersion?.let { "$KLIB_PROPERTY_METADATA_VERSION=$it" },
        builtInsPlatform?.let { "$KLIB_PROPERTY_BUILTINS_PLATFORM=${it.name}" },
    ).joinToString("\n")
}

private class KlibManifestComponentLayout(root: KlibFile) : KlibComponentLayout(root) {
    val manifestFile: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME).child(KLIB_MANIFEST_FILE_NAME)
}
