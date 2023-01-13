package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion

data class KotlinLibraryVersioning(
    val libraryVersion: String?,
    val compilerVersion: String?,
    val abiVersion: KotlinAbiVersion?,
    val metadataVersion: KlibMetadataVersion?,
)

fun Properties.writeKonanLibraryVersioning(versions: KotlinLibraryVersioning) {
    versions.abiVersion?.let { this.setProperty(KLIB_PROPERTY_ABI_VERSION, it.toString()) }
    versions.libraryVersion?.let { this.setProperty(KLIB_PROPERTY_LIBRARY_VERSION, it) }
    versions.compilerVersion?.let { this.setProperty(KLIB_PROPERTY_COMPILER_VERSION, it) }
    versions.metadataVersion?.let { this.setProperty(KLIB_PROPERTY_METADATA_VERSION, it.toString()) }
}

fun Properties.readKonanLibraryVersioning(): KotlinLibraryVersioning {
    val abiVersion = this.getProperty(KLIB_PROPERTY_ABI_VERSION)?.parseKotlinAbiVersion()
    val libraryVersion = this.getProperty(KLIB_PROPERTY_LIBRARY_VERSION)
    val compilerVersion = this.getProperty(KLIB_PROPERTY_COMPILER_VERSION)
    val metadataVersion = this.getProperty(KLIB_PROPERTY_METADATA_VERSION)?.let(KlibMetadataVersion::parse)

    return KotlinLibraryVersioning(
        abiVersion = abiVersion,
        libraryVersion = libraryVersion,
        compilerVersion = compilerVersion,
        metadataVersion = metadataVersion,
    )
}
