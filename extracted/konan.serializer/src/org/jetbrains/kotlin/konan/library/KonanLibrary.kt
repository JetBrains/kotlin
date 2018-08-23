package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.name.FqName

const val KLIB_PROPERTY_ABI_VERSION = "abi_version"
const val KLIB_PROPERTY_UNIQUE_NAME = "unique_name"
const val KLIB_PROPERTY_LINKED_OPTS = "linkerOpts"
const val KLIB_PROPERTY_DEPENDS = "depends"
const val KLIB_PROPERTY_INTEROP = "interop"
const val KLIB_PROPERTY_PACKAGE = "package"
const val KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS = "exportForwardDeclarations"
const val KLIB_PROPERTY_INCLUDED_HEADERS = "includedHeaders"

interface KonanLibrary {

    val libraryName: String
    val libraryFile: File

    // properties:
    val manifestProperties: Properties
    val abiVersion: String
    val linkerOpts: List<String>

    // paths:
    val bitcodePaths: List<String>
    val includedPaths: List<String>

    val targetList: List<String>

    val dataFlowGraph: ByteArray?
    val moduleHeaderData: ByteArray
    fun packageMetadata(fqName: String): ByteArray

    // FIXME: ddol: to be refactored into some global resolution context
    val isDefaultLibrary: Boolean get() = false
    val isNeededForLink: Boolean get() = true
    val resolvedDependencies: MutableList<KonanLibrary>
    fun markPackageAccessed(fqName: String)
}

val KonanLibrary.uniqueName
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)!!

val KonanLibrary.unresolvedDependencies: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS)

val KonanLibrary.isInterop
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INTEROP) == "true"

val KonanLibrary.packageFqName
    get() = manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)?.let { FqName(it) }

val KonanLibrary.exportForwardDeclarations
    get() = manifestProperties.getProperty(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS)
            .split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { FqName(it) }

val KonanLibrary.includedHeaders
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INCLUDED_HEADERS).split(' ')
