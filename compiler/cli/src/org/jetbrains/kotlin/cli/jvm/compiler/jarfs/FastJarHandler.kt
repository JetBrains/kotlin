/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

class FastJarHandler(val fileSystem: FastJarFileSystem, path: String) {
    private val myRoot: VirtualFile?
    internal val file = File(path)

    private val ourEntryMap: Map<String, ZipEntryDescription>
    private val cachedManifest: ByteArray?

    init {
        RandomAccessFile(file, "r").use { randomAccessFile ->
            val mappedByteBuffer = randomAccessFile.channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length())
            try {
                ourEntryMap = mappedByteBuffer.parseCentralDirectory().associateBy { it.relativePath }
                cachedManifest = ourEntryMap[MANIFEST_PATH]?.let(mappedByteBuffer::contentsToByteArray)
            } finally {
                with(fileSystem) {
                    mappedByteBuffer.unmapBuffer()
                }
            }
        }

        myRoot = FastJarVirtualFile(this, "", -1, null)

        val filesByRelativePath = HashMap<String, FastJarVirtualFile>(ourEntryMap.size)
        filesByRelativePath[""] = myRoot

        for (info in ourEntryMap.values) {
            getOrCreateFile(info, filesByRelativePath)
        }

        for (node in filesByRelativePath.values) {
            node.initChildrenArrayFromList()
        }
    }

    private fun getOrCreateFile(entry: ZipEntryDescription, map: MutableMap<String, FastJarVirtualFile>): FastJarVirtualFile {
        val entryName = entry.relativePath.normalizePath()

        return map.getOrPut(entryName) {
            val (parentName, shortName) = entryName.splitPath()

            val parentFile = getOrCreateDirectory(parentName, map)
            if ("." == shortName) {
                return parentFile
            }

            FastJarVirtualFile(
                this, shortName,
                if (entry.isDirectory) -1 else entry.uncompressedSize,
                parentFile
            )
        }
    }

    private fun getOrCreateDirectory(entryName: String, map: MutableMap<String, FastJarVirtualFile>): FastJarVirtualFile {
        return map.getOrPut(entryName) {
            val entry = ourEntryMap["$entryName/"]
            if (entry != null) {
                return getOrCreateFile(entry, map)
            }
            val (parentPath, shortName) = entryName.splitPath()
            require(entryName != parentPath) {
                "invalid entry name: '" + entryName + "' in " + this.file.absolutePath + "; after split: " + Pair(parentPath, shortName)
            }
            val parentFile = getOrCreateDirectory(parentPath, map)

            FastJarVirtualFile(this, shortName, -1, parentFile)
        }
    }

    private fun String.normalizePath(): String {
        if (endsWith('/')) return substring(0, length - 1).normalizePath()
        if (startsWith('/') || startsWith('\\')) return substring(1, length).normalizePath()
        if (!contains('\\')) return this
        return FileUtil.normalize(this)
    }

    private fun String.splitPath(): Pair<String, String> {
        val slashIndex = lastIndexOf('/')
        if (slashIndex == -1) return Pair("", this)
        return Pair(substring(0, slashIndex), substring(slashIndex + 1))
    }

    fun findFileByPath(pathInJar: String): VirtualFile? {
        return myRoot?.findFileByRelativePath(pathInJar)
    }

    fun contentsToByteArray(relativePath: String): ByteArray {
        if (relativePath == MANIFEST_PATH) return cachedManifest ?: throw FileNotFoundException("$file!/$relativePath")
        val zipEntryDescription = ourEntryMap[relativePath] ?: throw FileNotFoundException("$file!/$relativePath")
        return fileSystem.cachedOpenFileHandles[file].use {
            synchronized(it) {
                it.get().second.contentsToByteArray(zipEntryDescription)
            }
        }
    }
}

private const val MANIFEST_PATH = "META-INF/MANIFEST.MF"
