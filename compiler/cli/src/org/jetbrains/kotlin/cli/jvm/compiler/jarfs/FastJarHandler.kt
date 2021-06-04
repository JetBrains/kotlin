/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.util.containers.FactoryMap
import com.intellij.util.io.FileAccessorCache
import com.intellij.util.text.ByteArrayCharSequence
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FastJarHandler(val fileSystem: FastJarFileSystem, path: String) : ZipHandler(path) {
    private val myRoot: VirtualFile?

    private val ourEntryMap: Map<String, ZipEntryDescription>
    private val cachedManifest: ByteArray?

    init {
        RandomAccessFile(file, "r").use { randomAccessFile ->
            val mappedByteBuffer = randomAccessFile.channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length())
            ourEntryMap = mappedByteBuffer.parseCentralDirectory().associateBy { it.relativePath }
            cachedManifest = ourEntryMap[MANIFEST_PATH]?.let(mappedByteBuffer::contentsToByteArray)
        }

        val entries: MutableMap<EntryInfo, FastJarVirtualFile> = HashMap()
        val entriesMap = entriesMap
        val childrenMap = FactoryMap.create<FastJarVirtualFile, MutableList<VirtualFile>> { ArrayList() }
        for (info in entriesMap.values) {
            val file = getOrCreateFile(info, entries)
            val parent = file.parent
            if (parent != null) {
                childrenMap[parent]?.add(file)
            }
        }

        val rootInfo = getEntryInfo("")
        myRoot = rootInfo?.let { getOrCreateFile(it, entries) }

        for ((key, childList) in childrenMap) {
            key.children = childList.toTypedArray()
        }
    }

    private fun getOrCreateFile(info: EntryInfo, entries: MutableMap<EntryInfo, FastJarVirtualFile>): FastJarVirtualFile {
        var file = entries[info]
        if (file == null) {
            val parent = info.parent
            file = FastJarVirtualFile(this, info.shortName,
                                      if (info.isDirectory) -1 else info.length,
                                      info.timestamp,
                                      parent?.let { getOrCreateFile(it, entries) })
            entries[info] = file
        }
        return file
    }

    fun findFileByPath(pathInJar: String): VirtualFile? {
        return myRoot?.findFileByRelativePath(pathInJar)
    }

    @Throws(IOException::class)
    override fun createEntriesMap(): Map<String, EntryInfo> {
        val mapToEntryInfo = mutableMapOf<String, EntryInfo>()
        mapToEntryInfo[""] = EntryInfo("", true, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, null)
        for (zipEntry in ourEntryMap.values) {
            getOrCreate(zipEntry, mapToEntryInfo)
        }

        return mapToEntryInfo
    }

    private fun getOrCreate(entry: ZipEntryDescription, map: MutableMap<String, EntryInfo>): EntryInfo {
        var isDirectory = entry.isDirectory
        var entryName = entry.relativePath
        if (StringUtil.endsWithChar(entryName, '/')) {
            entryName = entryName.substring(0, entryName.length - 1)
            isDirectory = true
        }
        if (StringUtil.startsWithChar(entryName, '/') || StringUtil.startsWithChar(entryName, '\\')) {
            entryName = entryName.substring(1)
        }

        var info = map[entryName]
        if (info != null) return info

        val path = splitPathAndFix(entryName)

        val parentInfo = getOrCreateDirectory(path.first, map)
        if ("." == path.second) {
            return parentInfo
        }
        info = store(map, parentInfo, path.second, isDirectory, entry.uncompressedSize.toLong(), 0, path.third)
        return info
    }

    private fun getOrCreateDirectory(entryName: String, map: MutableMap<String, EntryInfo>): EntryInfo {
        var info = map[entryName]

        if (info == null) {
            val entry = ourEntryMap["$entryName/"]
            if (entry != null) {
                return getOrCreate(entry, map)
            }
            val path = splitPathAndFix(entryName)
            require(entryName != path.first) {
                "invalid entry name: '" + entryName + "' in " + this.file.absolutePath + "; after split: " + path
            }
            val parentInfo = getOrCreateDirectory(path.first, map)
            info = store(map, parentInfo, path.second, true, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, path.third)
        }

        return info
    }

    private fun store(
        map: MutableMap<String, EntryInfo>,
        parentInfo: EntryInfo?,
        shortName: CharSequence,
        isDirectory: Boolean,
        size: Long,
        time: Long,
        entryName: String
    ): EntryInfo {
        val sequence = ByteArrayCharSequence.convertToBytesIfPossible(shortName)
        val info = EntryInfo(sequence, isDirectory, size, time, parentInfo)
        map[entryName] = info
        return info
    }

    override fun contentsToByteArray(relativePath: String): ByteArray {
        if (relativePath == MANIFEST_PATH) return cachedManifest ?: throw FileNotFoundException("$file!/$relativePath")
        val zipEntryDescription = ourEntryMap[relativePath] ?: throw FileNotFoundException("$file!/$relativePath")
        return cachedOpenFileHandles[file].use {
            synchronized(it) {
                it.get().second.contentsToByteArray(zipEntryDescription)
            }
        }
    }

    companion object {
        fun cleanFileAccessorsCache() {
            cachedOpenFileHandles.clear()
        }
    }
}

private const val MANIFEST_PATH = "META-INF/MANIFEST.MF"

typealias RandomAccessFileAndBuffer = Pair<RandomAccessFile, MappedByteBuffer>

private val cachedOpenFileHandles: FileAccessorCache<File, RandomAccessFileAndBuffer> =
    object : FileAccessorCache<File, RandomAccessFileAndBuffer>(20, 10) {
        @Throws(IOException::class)
        override fun createAccessor(file: File): RandomAccessFileAndBuffer {
            val randomAccessFile = RandomAccessFile(file, "r")
            return Pair(randomAccessFile, randomAccessFile.channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length()))
        }

        @Throws(IOException::class)
        override fun disposeAccessor(fileAccessor: RandomAccessFileAndBuffer) {
            fileAccessor.first.close()
        }

        override fun isEqual(val1: File, val2: File): Boolean {
            return val1 == val2 // reference equality to handle different jars for different ZipHandlers on the same path
        }
    }
