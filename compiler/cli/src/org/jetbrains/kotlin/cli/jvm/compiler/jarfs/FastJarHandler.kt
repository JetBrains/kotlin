/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

class FastJarHandler(val fileSystem: FastJarFileSystem, path: String) {
    private val myRoot: VirtualFile?
    internal val file = File(path)

    private val cachedManifest: ByteArray?

    init {
        val entries: List<ZipEntryDescription>
        RandomAccessFile(file, "r").use { randomAccessFile ->
            val mappedByteBuffer = randomAccessFile.channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length())
            try {
                entries = mappedByteBuffer.parseCentralDirectory()
                cachedManifest =
                    entries.singleOrNull { StringUtil.equals(MANIFEST_PATH, it.relativePath) }
                        ?.let(mappedByteBuffer::contentsToByteArray)
            } finally {
                with(fileSystem) {
                    mappedByteBuffer.unmapBuffer()
                }
            }
        }

        myRoot = FastJarVirtualFile(this, "", -1, parent = null, entryDescription = null)

        // ByteArrayCharSequence should not be used instead of String
        // because the former class does not support equals/hashCode properly
        val filesByRelativePath = HashMap<String, FastJarVirtualFile>(entries.size)
        filesByRelativePath[""] = myRoot

        for (entryDescription in entries) {
            if (!entryDescription.isDirectory) {
                createFile(entryDescription, filesByRelativePath)
            } else {
                getOrCreateDirectory(entryDescription.relativePath, filesByRelativePath)
            }
        }

        for (node in filesByRelativePath.values) {
            node.initChildrenArrayFromList()
        }
    }

    private fun createFile(entry: ZipEntryDescription, directories: MutableMap<String, FastJarVirtualFile>): FastJarVirtualFile {
        val slashIndex = entry.relativePath.slashIndex()

        val parentFile = getOrCreateDirectory(entry.relativePath.getParentPath(slashIndex), directories)
        val shortName = entry.relativePath.getShortName(slashIndex)
        if ("." == shortName) {
            return parentFile
        }

        return FastJarVirtualFile(
            this, shortName,
            if (entry.isDirectory) -1 else entry.uncompressedSize,
            parentFile,
            entry,
        )
    }

    private fun getOrCreateDirectory(entryName: CharSequence, directories: MutableMap<String, FastJarVirtualFile>): FastJarVirtualFile {
        return directories.getOrPut(entryName.toString()) {
            val slashIndex = entryName.slashIndex()
            val parentFile = getOrCreateDirectory(entryName.getParentPath(slashIndex), directories)

            FastJarVirtualFile(this, entryName.getShortName(slashIndex), -1, parentFile, entryDescription = null)
        }
    }

    private fun CharSequence.getParentPath(slashIndex: Int): CharSequence = if (slashIndex == -1) "" else subSequence(0, slashIndex)

    private fun CharSequence.getShortName(slashIndex: Int): CharSequence =
        if (slashIndex == -1) this else subSequence(slashIndex + 1, this.length)

    private fun CharSequence.slashIndex(): Int {
        var slashIndex = this.length - 1

        while (slashIndex >= 0 && this[slashIndex] != '/') {
            slashIndex--
        }
        return slashIndex
    }

    fun findFileByPath(pathInJar: String): VirtualFile? {
        return myRoot?.findFileByRelativePath(pathInJar)
    }

    fun contentsToByteArray(zipEntryDescription: ZipEntryDescription): ByteArray {
        val relativePath = zipEntryDescription.relativePath
        if (StringUtil.equals(relativePath, MANIFEST_PATH)) return cachedManifest ?: throw FileNotFoundException("$file!/$relativePath")
        return fileSystem.cachedOpenFileHandles[file].use {
            synchronized(it) {
                it.get().second.contentsToByteArray(zipEntryDescription)
            }
        }
    }
}

private const val MANIFEST_PATH = "META-INF/MANIFEST.MF"
