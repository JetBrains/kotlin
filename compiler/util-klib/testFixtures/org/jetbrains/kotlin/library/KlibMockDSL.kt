/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import java.io.File
import kotlin.random.Random
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * A DSL to mock a Klib on the file system. See the default endpoint [mockKlib].
 */
class KlibMockDSL(val root: File) {
    fun dir(name: String, init: KlibMockDSL.() -> Unit = {}) {
        val dir = root.resolve(name).apply(File::mkdirs)
        KlibMockDSL(dir).init()
    }

    fun file(name: String, content: String = ""): Unit = root.resolve(name).writeText(content)
    fun file(name: String, content: ByteArray): Unit = root.resolve(name).writeBytes(content)

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
            KlibMockDSL(klibDir.resolve(KLIB_DEFAULT_COMPONENT_NAME).apply(File::mkdirs)).init()
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
        fun generateRandomPackageName(segmentsCount: Int): String {
            require(segmentsCount > 0) { "Length must be greater than zero" }

            val random = Random(System.nanoTime())

            return buildString {
                repeat(segmentsCount) {
                    if (isNotEmpty()) append('.')
                    append(generateRandomName(nameLength = random.nextInt(5, 10)))
                }
            }
        }

        private val SIMPLE_NAME_CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '_'
    }
}

fun KlibMockDSL.metadata(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_METADATA_FOLDER_NAME, init)

fun KlibMockDSL.metadata(metadata: SerializedMetadata) = metadata {
    file(KLIB_MODULE_METADATA_FILE_NAME, metadata.module)

    metadata.fragmentNames.forEachIndexed { index, packageName ->
        val fragmentDirName = if (packageName == "") "root_package" else "package_$packageName"
        val shortPackageName = packageName.substringAfterLast(".")

        dir(fragmentDirName) {
            val fragmentParts = metadata.fragments[index]

            val padding = fragmentParts.size.toString().length
            fun withPadding(fragmentPartIndex: Int) = String.format("%0${padding}d", fragmentPartIndex)

            fragmentParts.forEachIndexed { fragmentPartIndex, fragmentPart ->
                val fragmentPartFileName = "${withPadding(fragmentPartIndex)}_$shortPackageName$KLIB_METADATA_FILE_EXTENSION_WITH_DOT"
                file(fragmentPartFileName, fragmentPart)
            }
        }
    }
}

fun KlibMockDSL.resources(init: KlibMockDSL.() -> Unit = {}): Unit = dir(KLIB_RESOURCES_FOLDER_NAME, init)

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
    properties.saveToFile(KlibFile(root.resolve(KLIB_MANIFEST_FILE_NAME).path))
}
