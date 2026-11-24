/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.io.FileNotFoundException
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * The archive [path] with its parsed central directory and manifest.
 */
class JarArchive(
    /**
     * The path of the archive in the default file system, as only its files can be mapped.
     * Read outside this class only by the VFS-based implementation [FastJarVirtualFile].
     */
    internal val path: Path,
    unmapBuffer: MappedByteBuffer.() -> Unit,
    onInvalidArchive: (Path, Exception) -> Unit,
    private val readContents: (Path, ZipEntryDescription) -> ByteArray,
) {
    /** The entries of the central directory, in its order. */
    private val entries: List<ZipEntryDescription>

    private val cachedManifest: ByteArray?

    init {
        FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val largeBuffer =
                LargeDynamicMappedBuffer(
                    channel.size(),
                    { offset, size -> channel.map(FileChannel.MapMode.READ_ONLY, offset, size) },
                    unmapBuffer,
                    defaultByteOrder = ByteOrder.LITTLE_ENDIAN,
                )
            try {
                entries = try {
                    largeBuffer.parseCentralDirectory()
                } catch (e: Exception) {
                    onInvalidArchive(path, e)
                    emptyList()
                }
                cachedManifest =
                    entries.singleOrNull { MANIFEST_PATH.contentEquals(it.relativePath) }
                        ?.let(largeBuffer::contentsToByteArray)
            } finally {
                largeBuffer.unmap()
            }
        }
    }

    /**
     * Builds the tree of the entries of this archive with [factory] and returns its root.
     * Every parent of an entry is a directory, whether the archive lists it or not; each call builds a new tree.
     */
    fun <N : Any> buildTree(factory: JarTreeFactory<N>): N {
        val root = factory.createRoot()
        TreeBuilder(root, factory).apply {
            for (entryDescription in entries) {
                if (!entryDescription.isDirectory) {
                    createFile(entryDescription)
                } else {
                    getOrCreateDirectory(entryDescription.relativePath)
                }
            }
            initChildren()
        }
        return root
    }

    fun contentsToByteArray(entry: ZipEntryDescription): ByteArray {
        val relativePath = entry.relativePath
        if (MANIFEST_PATH.contentEquals(relativePath)) return cachedManifest ?: throw FileNotFoundException("$path!/$relativePath")
        return readContents(path, entry)
    }

    private class TreeBuilder<N : Any>(root: N, private val factory: JarTreeFactory<N>) {
        class Directory<N : Any>(val node: N) {
            val children = ArrayList<N>()
        }

        // ByteArrayCharSequence should not be used instead of String
        // because the former class does not support equals/hashCode properly
        private val directories = hashMapOf("" to Directory(root))

        fun createFile(entry: ZipEntryDescription) {
            val [parentName, shortName] = entry.relativePath.splitPath()

            val parentDirectory = getOrCreateDirectory(parentName)
            if ("." == shortName) {
                return
            }

            parentDirectory.children += factory.createFile(shortName, parentDirectory.node, entry)
        }

        fun getOrCreateDirectory(entryName: CharSequence): Directory<N> {
            return directories.getOrPut(entryName.toString()) {
                val [parentPath, shortName] = entryName.splitPath()
                val parentDirectory = getOrCreateDirectory(parentPath)

                Directory(factory.createDirectory(shortName, parentDirectory.node)).also { parentDirectory.children += it.node }
            }
        }

        fun initChildren() {
            for (directory in directories.values) {
                factory.setChildren(directory.node, directory.children)
            }
        }

        private fun CharSequence.splitPath(): Pair<CharSequence, CharSequence> {
            var slashIndex = this.length - 1

            while (slashIndex >= 0 && this[slashIndex] != '/') {
                slashIndex--
            }

            if (slashIndex == -1) return Pair("", this)
            return Pair(subSequence(0, slashIndex), subSequence(slashIndex + 1, this.length))
        }
    }
}

interface JarTreeFactory<N : Any> {
    fun createRoot(): N

    fun createDirectory(name: CharSequence, parent: N): N

    fun createFile(name: CharSequence, parent: N, entry: ZipEntryDescription): N

    /** Called once for each directory after all nodes are created, with its [children] in the order of the central directory. */
    fun setChildren(directory: N, children: List<N>)
}

private const val MANIFEST_PATH = "META-INF/MANIFEST.MF"
