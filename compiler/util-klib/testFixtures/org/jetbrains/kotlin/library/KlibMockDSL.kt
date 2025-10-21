/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_BODIES_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_DEBUG_INFO_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_DECLARATIONS_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_FILES_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_FILE_ENTRIES_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_SIGNATURES_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_STRINGS_FILE_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout.Companion.IR_TYPES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.IrArrayWriter
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import java.io.File
import kotlin.collections.map
import kotlin.random.Random
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * A DSL to mock a Klib on the file system. See the default endpoint [mockKlib].
 */
class KlibMockDSL(val currentDir: File, val parent: KlibMockDSL?) {
    fun dir(name: String, init: KlibMockDSL.() -> Unit = {}) {
        val newDir = currentDir.resolve(name).apply(File::mkdirs)
        KlibMockDSL(currentDir = newDir, parent = this).init()
    }

    fun file(name: String, content: String = ""): Unit = currentDir.resolve(name).writeText(content)
    fun file(name: String, content: ByteArray): Unit = currentDir.resolve(name).writeBytes(content)

    val rootDir: File
        get() = parent?.rootDir ?: currentDir

    companion object {
        /**
         * The default endpoint:
         * ```
         * val mock = mockKlib(klibDir) {
         *     metadata()
         *     manifest()
         *     ...
         * }
         * ```
         */
        fun mockKlib(klibDir: File, init: KlibMockDSL.() -> Unit): File {
            klibDir.mkdirs()
            KlibMockDSL(currentDir = klibDir, parent = null).apply {
                dir(KLIB_DEFAULT_COMPONENT_NAME, init)
            }
            return klibDir
        }

        /** Generates a random metadata to be consumed by [metadata]. */
        fun generateRandomMetadata(): SerializedMetadata {
            val random = Random(System.nanoTime())

            val fragmentsCount = random.nextInt(3, 5)

            val fragments = mutableListOf<List<ByteArray>>()
            val fragmentNames = mutableListOf<String>()

            repeat(fragmentsCount) { index ->
                // Always include the root package for index=0.
                val packageName = if (index == 0) "" else generateRandomPackageName(segmentsCount = random.nextInt(1, 4))

                fragmentNames += packageName
                fragments += List(random.nextInt(1, 5)) { random.nextBytes(100) }
            }

            return SerializedMetadata(
                module = random.nextBytes(100),
                fragments = fragments,
                fragmentNames = fragmentNames,
            )
        }

        /** Generates a random "IR file" to be consumed by [ir] and [irInlinableFunctions]. */
        fun generateRandomIrFile(): SerializedIrFile {
            val random = Random(System.nanoTime())

            return SerializedIrFile(
                fileData = random.nextBytes(100),
                fqName = generateRandomPackageName(segmentsCount = random.nextInt(1, 4)),
                path = generateFictitiousRandomPath(segmentsCount = random.nextInt(1, 4)),
                types = random.nextBytes(1_000),
                signatures = random.nextBytes(10_000),
                strings = random.nextBytes(1_000),
                bodies = random.nextBytes(100_000),
                declarations = random.nextBytes(10_000),
                debugInfo = random.nextBytes(10_000),
                backendSpecificMetadata = null, // never written to the disk
                fileEntries = random.nextBytes(1_000)
            )
        }

        /** Generates a random name. */
        fun generateRandomName(nameLength: Int): String {
            require(nameLength > 0) { "Length must be greater than zero" }

            val random = Random(System.nanoTime())

            return buildString {
                while (this.length < nameLength) {
                    val nextChar = SIMPLE_NAME_CHARS.random(random)
                    if (this.isEmpty() && !nextChar.isJavaIdentifierStart()) continue
                    append(nextChar)
                }
            }
        }

        /** Generates a random package name. */
        fun generateRandomPackageName(segmentsCount: Int): String = generateRandomPathLikeString(segmentsCount) {
            if (isNotEmpty()) append('.')
        }

        private inline fun generateRandomPathLikeString(segmentsCount: Int, addSeparator: StringBuilder.() -> Unit): String {
            require(segmentsCount > 0) { "Length must be greater than zero" }

            val random = Random(System.nanoTime())

            return buildString {
                repeat(segmentsCount) {
                    addSeparator()
                    append(generateRandomName(nameLength = random.nextInt(5, 10)))
                }
            }
        }

        private fun generateFictitiousRandomPath(segmentsCount: Int) = generateRandomPathLikeString(segmentsCount) { append('/') }

        private val SIMPLE_NAME_CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '_'
    }
}

fun KlibMockDSL.manifest(
    uniqueName: String,
    builtInsPlatform: BuiltInsPlatform,
    versioning: KotlinLibraryVersioning,
    other: Properties.() -> Unit = {},
) {
    val properties = Properties()
    properties[KLIB_PROPERTY_UNIQUE_NAME] = uniqueName
    if (builtInsPlatform != BuiltInsPlatform.COMMON) {
        properties[KLIB_PROPERTY_BUILTINS_PLATFORM] = builtInsPlatform.name
    }
    properties.writeKonanLibraryVersioning(versioning)
    properties.other()
    properties.saveToFile(KlibFile(currentDir.resolve(KLIB_MANIFEST_FILE_NAME).path))
}

fun KlibMockDSL.resources(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_RESOURCES_FOLDER_NAME, init)

fun KlibMockDSL.metadata(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_METADATA_FOLDER_NAME, init)

// TODO (KT-81411): rewrite it on the new metadata component layout
fun KlibMockDSL.metadata(metadata: SerializedMetadata) = metadata {
    file(KLIB_MODULE_METADATA_FILE_NAME, metadata.module)

    metadata.fragmentNames.forEachIndexed { index, packageName ->
        val fragmentDirName = if (packageName == "")
            KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
        else
            "$KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX$packageName"
        val shortPackageName = packageName.substringAfterLast(".")

        dir(fragmentDirName) {
            val fragmentParts = metadata.fragments[index]

            val padding = fragmentParts.size.toString().length
            fun withPadding(fragmentPartIndex: Int) = String.format("%0${padding}d", fragmentPartIndex)

            fragmentParts.forEachIndexed { fragmentPartIndex, fragmentPart ->
                val fragmentPartFileName = "${withPadding(fragmentPartIndex)}_$shortPackageName.$KLIB_METADATA_FILE_EXTENSION"
                file(fragmentPartFileName, fragmentPart)
            }
        }
    }
}

fun KlibMockDSL.ir(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_IR_FOLDER_NAME, init)

fun KlibMockDSL.ir(irFiles: Collection<SerializedIrFile>) = ir {
    serializeIr(irFiles)
}

fun KlibMockDSL.irInlinableFunctions(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_IR_INLINABLE_FUNCTIONS_DIR_NAME, init)

fun KlibMockDSL.irInlinableFunctions(inlinableFunctionsFile: SerializedIrFile) = irInlinableFunctions {
    serializeIr(listOf(inlinableFunctionsFile))
}

// TODO (KT-81411): rewrite it on the new ir component layout
private fun KlibMockDSL.serializeIr(irFiles: Collection<SerializedIrFile>) {
    fun pathOf(name: String): String = currentDir.resolve(name).absolutePath

    with(irFiles.sortedBy { it.path }) {
        IrArrayWriter(map(SerializedIrFile::fileData)).writeIntoFile(pathOf(IR_FILES_FILE_NAME))
        IrArrayWriter(map(SerializedIrFile::declarations)).writeIntoFile(pathOf(IR_DECLARATIONS_FILE_NAME))
        IrArrayWriter(map(SerializedIrFile::types)).writeIntoFile(pathOf(IR_TYPES_FILE_NAME))
        IrArrayWriter(map(SerializedIrFile::signatures)).writeIntoFile(pathOf(IR_SIGNATURES_FILE_NAME))
        IrArrayWriter(map(SerializedIrFile::strings)).writeIntoFile(pathOf(IR_STRINGS_FILE_NAME))
        IrArrayWriter(map(SerializedIrFile::bodies)).writeIntoFile(pathOf(IR_BODIES_FILE_NAME))

        mapNotNull(SerializedIrFile::debugInfo).let { debugInfos ->
            if (debugInfos.isNotEmpty()) {
                check(debugInfos.size == size) { "debugInfo.size != irFiles.size" }
                IrArrayWriter(debugInfos).writeIntoFile(pathOf(IR_DEBUG_INFO_FILE_NAME))
            }
        }

        mapNotNull(SerializedIrFile::fileEntries).let { fileEntries ->
            if (fileEntries.isNotEmpty()) {
                check(fileEntries.size == size) { "fileEntries.size != irFiles.size" }
                IrArrayWriter(fileEntries).writeIntoFile(pathOf(IR_FILE_ENTRIES_FILE_NAME))
            }
        }
    }
}
