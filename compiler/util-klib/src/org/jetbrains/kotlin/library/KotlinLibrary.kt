package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform

/**
 * [org.jetbrains.kotlin.library.KotlinAbiVersion]
 */
const val KLIB_PROPERTY_ABI_VERSION = "abi_version"
const val KLIB_PROPERTY_COMPILER_VERSION = "compiler_version"

/**
 * A set of values of [org.jetbrains.kotlin.library.KotlinIrSignatureVersion].
 */
const val KLIB_PROPERTY_IR_SIGNATURE_VERSIONS = "ir_signature_versions"

/**
 * [org.jetbrains.kotlin.library.metadata.KlibMetadataVersion]
 */
const val KLIB_PROPERTY_METADATA_VERSION = "metadata_version"

@Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS)
const val KLIB_PROPERTY_DEPENDENCY_VERSION = "dependency_version"

@Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS)
const val KLIB_PROPERTY_LIBRARY_VERSION = "library_version"

const val KLIB_PROPERTY_UNIQUE_NAME = "unique_name"
const val KLIB_PROPERTY_SHORT_NAME = "short_name"
const val KLIB_PROPERTY_DEPENDS = "depends"
const val KLIB_PROPERTY_PACKAGE = "package"
const val KLIB_PROPERTY_BUILTINS_PLATFORM = "builtins_platform"
const val KLIB_PROPERTY_CONTAINS_ERROR_CODE = "contains_error_code"

// Native-specific:
const val KLIB_PROPERTY_INTEROP = "interop"
const val KLIB_PROPERTY_HEADER = "header"
const val KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS = "exportForwardDeclarations"
const val KLIB_PROPERTY_INCLUDED_FORWARD_DECLARATIONS = "includedForwardDeclarations"
const val KLIB_PROPERTY_IR_PROVIDER = "ir_provider"

/**
 * Copy-pasted to `kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/Utils.kt`
 */
const val KLIB_PROPERTY_NATIVE_TARGETS = "native_targets"
const val KLIB_PROPERTY_WASM_TARGETS = "wasm_targets"

// Commonizer-specific:
/**
 * Identity String of the commonizer target representing this artifact.
 * This will also include native targets that were absent during commonization
 */
const val KLIB_PROPERTY_COMMONIZER_TARGET = "commonizer_target"

/**
 * Similar to [KLIB_PROPERTY_NATIVE_TARGETS] but this will also preserve targets
 * that were unsupported on the host creating this artifact
 */
const val KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS = "commonizer_native_targets"

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
}

interface MetadataLibrary {
    val moduleHeaderData: ByteArray
    fun packageMetadataParts(fqName: String): Set<String>
    fun packageMetadata(fqName: String, partName: String): ByteArray
}

interface IrLibrary {
    val hasIr: Boolean
    fun irDeclaration(index: Int, fileIndex: Int): ByteArray
    fun type(index: Int, fileIndex: Int): ByteArray
    fun signature(index: Int, fileIndex: Int): ByteArray
    fun string(index: Int, fileIndex: Int): ByteArray
    fun body(index: Int, fileIndex: Int): ByteArray
    fun debugInfo(index: Int, fileIndex: Int): ByteArray?
    fun file(index: Int): ByteArray
    fun fileCount(): Int

    fun types(fileIndex: Int): ByteArray
    fun signatures(fileIndex: Int): ByteArray
    fun strings(fileIndex: Int): ByteArray
    fun declarations(fileIndex: Int): ByteArray
    fun bodies(fileIndex: Int): ByteArray
}

val BaseKotlinLibrary.isNativeStdlib: Boolean
    get() = uniqueName == KOTLIN_NATIVE_STDLIB_NAME && builtInsPlatform == BuiltInsPlatform.NATIVE

val BaseKotlinLibrary.isJsStdlib: Boolean
    get() = uniqueName == KOTLIN_JS_STDLIB_NAME && builtInsPlatform == BuiltInsPlatform.JS

val BaseKotlinLibrary.isWasmStdlib: Boolean
    get() = uniqueName == KOTLIN_WASM_STDLIB_NAME && builtInsPlatform == BuiltInsPlatform.WASM

val BaseKotlinLibrary.uniqueName: String
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)!!

val BaseKotlinLibrary.shortName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SHORT_NAME)

val BaseKotlinLibrary.unresolvedDependencies: List<RequiredUnresolvedLibrary>
    get() = unresolvedDependencies(lenient = false).map { it as RequiredUnresolvedLibrary }

fun BaseKotlinLibrary.unresolvedDependencies(lenient: Boolean = false): List<UnresolvedLibrary> =
    manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true)
        .map { UnresolvedLibrary(it, lenient = lenient) }

val BaseKotlinLibrary.hasDependencies: Boolean
    get() = !manifestProperties.getProperty(KLIB_PROPERTY_DEPENDS).isNullOrBlank()

interface KotlinLibrary : BaseKotlinLibrary, MetadataLibrary, IrLibrary

@Deprecated(
    "Use BaseKotlinLibrary.isCInteropLibrary() for more precise check",
    ReplaceWith("isCInteropLibrary()", "org.jetbrains.kotlin.library.metadata.isCInteropLibrary"),
    DeprecationLevel.WARNING
)
val KotlinLibrary.isInterop: Boolean get() = interopFlag == "true"

val BaseKotlinLibrary.interopFlag: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INTEROP)

val KotlinLibrary.isHeader: Boolean
    get() = manifestProperties.getProperty(KLIB_PROPERTY_HEADER) == "true"

val KotlinLibrary.packageFqName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)

val KotlinLibrary.exportForwardDeclarations: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS, escapeInQuotes = true)

val KotlinLibrary.includedForwardDeclarations: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_INCLUDED_FORWARD_DECLARATIONS, escapeInQuotes = true)

val BaseKotlinLibrary.irProviderName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_IR_PROVIDER)

val BaseKotlinLibrary.nativeTargets: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_NATIVE_TARGETS)

val BaseKotlinLibrary.wasmTargets: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_WASM_TARGETS)

val KotlinLibrary.containsErrorCode: Boolean
    get() = manifestProperties.getProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE) == "true"

@Deprecated("Use BaseKotlinLibrary.commonizerTarget instead", level = DeprecationLevel.HIDDEN)
val KotlinLibrary.commonizerTarget: String?
    get() = commonizerTarget

val BaseKotlinLibrary.commonizerTarget: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_COMMONIZER_TARGET)

@Deprecated("Use BaseKotlinLibrary.builtInsPlatform instead", level = DeprecationLevel.HIDDEN)
val KotlinLibrary.builtInsPlatform: String?
    get() = builtInsPlatform?.name

val BaseKotlinLibrary.builtInsPlatform: BuiltInsPlatform?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_BUILTINS_PLATFORM)?.let(BuiltInsPlatform::parseFromString)

val BaseKotlinLibrary.commonizerNativeTargets: List<String>?
    get() = if (manifestProperties.containsKey(KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS))
        manifestProperties.propertyList(KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS, escapeInQuotes = true)
    else null

const val DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS = "Library and dependency versions have been phased out, see KT-65834"
