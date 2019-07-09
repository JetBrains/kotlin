package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.parseKonanVersion
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.KonanVersion

data class KonanLibraryVersioning(
    val libraryVersion: String?,
    val compilerVersion: KonanVersion?,
    val abiVersion: KotlinAbiVersion?
)

fun Properties.writeKonanLibraryVersioning(versions: KonanLibraryVersioning) {
    versions.abiVersion ?. let { this.setProperty(KLIB_PROPERTY_ABI_VERSION, it.toString()) }
    versions.libraryVersion ?. let { this.setProperty(KLIB_PROPERTY_LIBRARY_VERSION, it) }
    versions.compilerVersion ?. let { this.setProperty(KLIB_PROPERTY_COMPILER_VERSION, "${versions.compilerVersion.toString(true, true)}") }
}

fun Properties.readKonanLibraryVersioning(): KonanLibraryVersioning {
    val abiVersion = this.getProperty(KLIB_PROPERTY_ABI_VERSION)?.parseKonanAbiVersion()
    val libraryVersion = this.getProperty(KLIB_PROPERTY_LIBRARY_VERSION)
    val compilerVersion = this.getProperty(KLIB_PROPERTY_COMPILER_VERSION)?.parseKonanVersion()

    return KonanLibraryVersioning(
        abiVersion = abiVersion,
        libraryVersion = libraryVersion,
        compilerVersion = compilerVersion
    )
}
