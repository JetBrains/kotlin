package org.jetbrains.kotlin.konan.library

interface MetadataReader {
    fun loadSerializedModule(library: KonanLibrary): ByteArray
    fun loadSerializedPackageFragment(library: KonanLibrary, fqName: String): ByteArray
}
