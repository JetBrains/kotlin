package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties

interface KonanLibraryReader {

    val libraryFile: File
    val libraryName: String

    val manifestProperties: Properties
    val uniqueName: String
    val linkerOpts: List<String>
    val unresolvedDependencies: List<String>
    val bitcodePaths: List<String>
    val includedPaths: List<String>

    val dataFlowGraph: ByteArray?
    val moduleHeaderData: ByteArray
    fun packageMetadata(fqName: String): ByteArray

    val isDefaultLibrary: Boolean get() = false

    // FIXME: ddol: to be refactored into some global resolution context
    val isNeededForLink: Boolean get() = true
    val resolvedDependencies: MutableList<KonanLibraryReader>
    fun markPackageAccessed(fqName: String)
}
