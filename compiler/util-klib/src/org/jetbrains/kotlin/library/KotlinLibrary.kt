package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList

const val KLIB_PROPERTY_ABI_VERSION = "abi_version"
const val KLIB_PROPERTY_COMPILER_VERSION = "compiler_version"
const val KLIB_PROPERTY_DEPENDENCY_VERSION = "dependency_version"
const val KLIB_PROPERTY_LIBRARY_VERSION = "library_version"
const val KLIB_PROPERTY_METADATA_VERSION = "metadata_version"
const val KLIB_PROPERTY_IR_VERSION = "ir_version"
const val KLIB_PROPERTY_UNIQUE_NAME = "unique_name"
const val KLIB_PROPERTY_DEPENDS = "depends"
const val KLIB_PROPERTY_PACKAGE = "package"
const val KLIB_PROPERTY_INTEROP = "interop"
const val KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS = "exportForwardDeclarations"

/**
 * Abstractions for getting access to the information stored inside of Kotlin/Native library.
 */

interface BaseKotlinLibrary {
    val libraryName: String
    val libraryFile: File
    val componentList: List<String>
    val versions: KotlinLibraryVersioning
    // Whether this library is default (provided by distribution)?
    val isDefault: Boolean
    val manifestProperties: Properties
    val has_pre_1_4_manifest: Boolean
}

interface MetadataLibrary {
    val moduleHeaderData: ByteArray
    fun packageMetadataParts(fqName: String): Set<String>
    fun packageMetadata(fqName: String, partName: String): ByteArray
}

interface IrLibrary {
    val dataFlowGraph: ByteArray?
    fun irDeclaration(index: Long, fileIndex: Int): ByteArray
    fun symbol(index: Int, fileIndex: Int): ByteArray
    fun type(index: Int, fileIndex: Int): ByteArray
    fun string(index: Int, fileIndex: Int): ByteArray
    fun body(index: Int, fileIndex: Int): ByteArray
    fun file(index: Int): ByteArray
    fun fileCount(): Int
}

val BaseKotlinLibrary.uniqueName: String
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)!!

val BaseKotlinLibrary.unresolvedDependencies: List<UnresolvedLibrary>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true)
        .map { UnresolvedLibrary(it, manifestProperties.getProperty("dependency_version_$it")) }

interface KotlinLibrary : BaseKotlinLibrary, MetadataLibrary, IrLibrary

// TODO: should we move the below ones to Native?
val KotlinLibrary.isInterop
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INTEROP) == "true"

val KotlinLibrary.packageFqName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)

val KotlinLibrary.exportForwardDeclarations
    get() = manifestProperties.propertyList(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS, escapeInQuotes = true)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
