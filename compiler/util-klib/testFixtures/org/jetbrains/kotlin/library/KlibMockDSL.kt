/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.impl.KlibIrWriterImpl
import org.jetbrains.kotlin.library.impl.KlibMetadataWriterImpl
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.io.File
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
                metadataVersion = MetadataVersion.INSTANCE.toArray(),
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

fun KlibMockDSL.metadata(metadata: SerializedMetadata) {
    val layout = KlibMetadataComponentLayout(rootDir.path)
    val writer = KlibMetadataWriterImpl(layout)
    writer.writeMetadata(metadata)
}

fun KlibMockDSL.ir(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_IR_FOLDER_NAME, init)

fun KlibMockDSL.ir(irFiles: Collection<SerializedIrFile>) = ir {
    val layout = KlibIrComponentLayout.createForMainIr(KlibFile(rootDir.path))
    val writer = KlibIrWriterImpl.ForMainIr(layout)
    writer.writeMainIr(irFiles)
}

fun KlibMockDSL.irInlinableFunctions(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME, init)

fun KlibMockDSL.irInlinableFunctions(inlinableFunctionsFile: SerializedIrFile) = irInlinableFunctions {
    val layout = KlibIrComponentLayout.createForInlinableFunctionsIr(KlibFile(rootDir.path))
    val writer = KlibIrWriterImpl.ForInlinableFunctionsIr(layout)
    writer.writeInlinableFunctionsIr(inlinableFunctionsFile)
}
