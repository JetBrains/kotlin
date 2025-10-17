/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.KlibFileSystemDiff.Message.*
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

/**
 * Do the recursive "diff" of the two Klib directories on the file system: [leftRoot] and [rightRoot].
 *
 * The entry point is the [recursiveDiff] function, which returns either [Result.Identical] or [Result.Different].
 */
class KlibFileSystemDiff(
    private val leftRoot: File,
    private val rightRoot: File
) {
    /** The result of Klib directory comparison */
    sealed interface Result {
        /** Identical Klibs */
        object Identical : Result

        /** Klibs are different. All the differences are listed as textual descriptions in [differences]. */
        class Different(
            private val leftRoot: File,
            private val rightRoot: File,
            val differences: List<String>
        ) : Result {
            override fun toString() = buildString {
                appendLine("${differences.size} differences found while comparing $leftRoot and $rightRoot:")
                differences.forEach { appendLine(it) }
            }
        }
    }

    private enum class FileSystemElementType { REGULAR_FILE, DIRECTORY, OTHER }

    private data class FileSystemElement(val root: File, val path: File = root) {
        val type: FileSystemElementType by lazy {
            val attributes = Files.readAttributes(path.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            when {
                attributes.isRegularFile -> FileSystemElementType.REGULAR_FILE
                attributes.isDirectory -> FileSystemElementType.DIRECTORY
                else -> FileSystemElementType.OTHER
            }
        }

        val manifestProperties: Map<String, String> by lazy {
            val properties = Properties().apply { path.bufferedReader().use { load(it) } }

            val keys = properties.keys.mapTo(hashSetOf()) { key ->
                key as? String ?: error("Manifest ${this.path} contains a non-String key: $key")
            }

            buildMap {
                for (key in keys) {
                    this[key] = properties.getProperty(key)
                        ?: error("Manifest ${this@FileSystemElement.path} doesn't contain a value for key $key")
                }
            }
        }

        val children: Map<String, FileSystemElement> by lazy {
            path.listFiles().orEmpty().associate { child ->
                child.name to this.copy(path = child)
            }
        }

        val looksLikeManifest: Boolean
            get() = path.name == KLIB_MANIFEST_FILE_NAME

        fun contentEquals(other: FileSystemElement): Boolean {
            return path.length() == other.path.length() && path.readBytes().contentEquals(other.path.readBytes())
        }

        override fun toString(): String = path.relativeTo(root).path
    }

    private sealed class Message {
        abstract override fun toString(): String

        class DifferentElementTypes(val left: FileSystemElement, val right: FileSystemElement) : Message() {
            override fun toString() = "[!] $left files have different types: ${left.type} != ${right.type}"
        }

        class DifferentContents(val any: FileSystemElement) : Message() {
            override fun toString() = "[!] $any files have a different content"
        }

        class MissingElement(val any: FileSystemElement, val isLeft: Boolean) : Message() {
            override fun toString() = "[${if (isLeft) '<' else '>'}] $any is missing"
        }

        class MissingManifestProperty(val any: FileSystemElement, val key: String, val value: String, val isLeft: Boolean) : Message() {
            override fun toString() = "[${if (isLeft) '<' else '>'}] $any doesn't have a property: $key -> $value"
        }

        class DifferentManifestProperties(
            val any: FileSystemElement, val key: String, val leftValue: String, val rightValue: String
        ) : Message() {
            override fun toString() = "[!] $any properties differ:\n\t$key -> $leftValue\n\t$key -> $rightValue"
        }
    }

    fun recursiveDiff(): Result {
        require(leftRoot.exists()) { "Klib does not exist: $leftRoot" }
        require(rightRoot.exists()) { "Klib does not exist: $rightRoot" }

        val messages = mutableListOf<Message>()
        processFileSystemElements(FileSystemElement(leftRoot), FileSystemElement(rightRoot), messages)

        return if (messages.isEmpty())
            Result.Identical
        else
            Result.Different(leftRoot, rightRoot, messages.map { it.toString() })
    }

    private fun processFileSystemElements(left: FileSystemElement, right: FileSystemElement, messages: MutableList<Message>) {
        when {
            left.type != right.type -> messages += DifferentElementTypes(left, right)
            left.type == FileSystemElementType.REGULAR_FILE -> processRegularFiles(left, right, messages)
            left.type == FileSystemElementType.DIRECTORY -> processDirectories(left, right, messages)
            else -> error("Unsupported file system element type: ${left.type}")
        }
    }

    private fun processRegularFiles(left: FileSystemElement, right: FileSystemElement, messages: MutableList<Message>) {
        if (left.looksLikeManifest) {
            processManifestFiles(left, right, messages)
        } else if (!left.contentEquals(right)) {
            messages += DifferentContents(left)
        }
    }

    private fun processManifestFiles(left: FileSystemElement, right: FileSystemElement, messages: MutableList<Message>) {
        val leftProperties = left.manifestProperties
        val writtenProperties = right.manifestProperties

        val allKeys = buildSet<String> {
            this += leftProperties.keys
            this += writtenProperties.keys
        }.sorted()

        for (key in allKeys) {
            val leftValue = leftProperties[key]
            val rightValue = writtenProperties[key]

            when {
                leftValue == null -> messages += MissingManifestProperty(right, key, rightValue!!, isLeft = true)
                rightValue == null -> messages += MissingManifestProperty(left, key, leftValue, isLeft = false)
                leftValue != rightValue -> messages += DifferentManifestProperties(left, key, leftValue, rightValue)
            }
        }
    }

    private fun processDirectories(left: FileSystemElement, right: FileSystemElement, messages: MutableList<Message>) {
        val leftChildren: Map<String, FileSystemElement> = left.children
        val rightChildren: Map<String, FileSystemElement> = right.children

        if (leftChildren.isNotEmpty() || rightChildren.isNotEmpty()) {
            val allNames: List<String> = buildSet<String> {
                this += leftChildren.keys
                this += rightChildren.keys
            }.sorted()

            for (name in allNames) {
                val leftChild = leftChildren[name]
                val rightChild = rightChildren[name]

                when {
                    leftChild == null -> messages += MissingElement(rightChild!!, isLeft = true)
                    rightChild == null -> messages += MissingElement(leftChild, isLeft = false)
                    else -> processFileSystemElements(leftChild, rightChild, messages)
                }
            }
        }
    }
}
