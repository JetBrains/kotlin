/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap

// The sizes of the FileAccessorCache that FastJar used before.
private const val PROTECTED_OPEN_ARCHIVES = 20
private const val PROBATIONAL_OPEN_ARCHIVES = 10

class JarArchiveCache(
    private val unmapBuffer: MappedByteBuffer.() -> Unit,
    private val onInvalidArchive: (File, Exception) -> Unit,
) {
    private val archives = ConcurrentHashMap<String, JarArchive>()

    private val droppedArchives = ArrayList<OpenArchive>()

    private val openArchives = SegmentedLruMap<File, OpenArchive>(PROTECTED_OPEN_ARCHIVES, PROBATIONAL_OPEN_ARCHIVES) {
        droppedArchives += it
    }

    fun archive(path: String): JarArchive {
        archives[path]?.let { return it }
        // Parsed outside the map, so that parsing one archive does not block requests for others.
        val archive = JarArchive(File(path), unmapBuffer, onInvalidArchive, ::readContents)
        return archives.putIfAbsent(path, archive) ?: archive
    }

    fun clear() {
        archives.clear()
        closeOpenArchives()
    }

    fun closeOpenArchives() {
        synchronized(openArchives) {
            openArchives.clear()
        }
        releaseDroppedArchives()
    }

    private fun readContents(file: File, entry: ZipEntryDescription): ByteArray {
        val openArchive = acquire(file)
        try {
            return synchronized(openArchive) {
                openArchive.buffer.contentsToByteArray(entry)
            }
        } finally {
            release(openArchive)
        }
    }

    private fun acquire(file: File): OpenArchive {
        synchronized(openArchives) {
            openArchives[file]?.let {
                it.referenceCount++
                return it
            }
        }
        val created = OpenArchive(file)
        val existing = synchronized(openArchives) {
            val existing = openArchives[file]
            if (existing != null) {
                existing.referenceCount++
            } else {
                // One reference of the cache and one of the reader.
                created.referenceCount = 2
                openArchives.put(file, created)
            }
            existing
        }
        releaseDroppedArchives()
        if (existing == null) return created
        // Another reader has opened the archive meanwhile.
        created.close()
        return existing
    }

    private fun releaseDroppedArchives() {
        val dropped = synchronized(openArchives) {
            if (droppedArchives.isEmpty()) return
            droppedArchives.toList().also { droppedArchives.clear() }
        }
        dropped.forEach(::release)
    }

    private fun release(openArchive: OpenArchive) {
        val unused = synchronized(openArchives) { --openArchive.referenceCount == 0 }
        if (unused) openArchive.close()
    }

    private inner class OpenArchive(file: File) {
        private val randomAccessFile = RandomAccessFile(file, "r")

        val buffer = LargeDynamicMappedBuffer(
            randomAccessFile.length(),
            { offset, size -> randomAccessFile.channel.map(FileChannel.MapMode.READ_ONLY, offset, size) },
            unmapBuffer,
            defaultByteOrder = ByteOrder.LITTLE_ENDIAN,
        )

        /** Guarded by [openArchives]. */
        var referenceCount = 0

        fun close() {
            randomAccessFile.close()
            buffer.unmap()
        }
    }
}

/**
 * A segmented LRU map, as the IntelliJ `SLRUMap` behind the `FileAccessorCache`: a new value goes to the probational queue,
 * and moves to the protected queue when it is requested again. A value dropped from the protected queue goes back to the
 * probational one, and only a value dropped from the probational queue, replaced or cleared, is passed to [onDrop].
 * So values requested once cannot push the values requested repeatedly out of the map.
 *
 * Not thread-safe.
 */
private class SegmentedLruMap<K : Any, V : Any>(
    private val protectedSize: Int,
    private val probationalSize: Int,
    private val onDrop: (V) -> Unit,
) {
    private val probationalQueue = object : LinkedHashMap<K, V>(probationalSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
            if (size <= probationalSize) return false
            onDrop(eldest.value)
            return true
        }
    }

    private val protectedQueue = object : LinkedHashMap<K, V>(protectedSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
            if (size <= protectedSize) return false
            probationalQueue[eldest.key] = eldest.value
            return true
        }
    }

    operator fun get(key: K): V? =
        protectedQueue[key] ?: probationalQueue.remove(key)?.also { protectedQueue[key] = it }

    fun put(key: K, value: V) {
        protectedQueue.remove(key)?.let(onDrop)
        probationalQueue.put(key, value)?.let(onDrop)
    }

    fun clear() {
        protectedQueue.values.forEach(onDrop)
        probationalQueue.values.forEach(onDrop)
        protectedQueue.clear()
        probationalQueue.clear()
    }
}
