/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

// The sizes of the FileAccessorCache that FastJar used before.
private const val PROTECTED_OPEN_ARCHIVES = 20
private const val PROBATIONAL_OPEN_ARCHIVES = 10

class JarArchiveCache(
    private val unmapBuffer: MappedByteBuffer.() -> Unit,
    private val onInvalidArchive: (Path, Exception) -> Unit,
) {
    private val archives = ConcurrentHashMap<String, JarArchive>()

    private val droppedArchives = ArrayList<OpenArchive>()

    private val openArchives = SegmentedLruMap<Path, OpenArchive>(PROTECTED_OPEN_ARCHIVES, PROBATIONAL_OPEN_ARCHIVES) {
        droppedArchives += it
    }

    operator fun get(path: String): JarArchive {
        archives[path]?.let { return it }
        // Parsed outside the map, so that parsing one archive does not block requests for others.
        val archive = JarArchive(Paths.get(path), unmapBuffer, onInvalidArchive, ::readContents)
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

    private fun readContents(path: Path, entry: ZipEntryDescription): ByteArray {
        val openArchive = acquire(path)
        try {
            return synchronized(openArchive) {
                openArchive.buffer.contentsToByteArray(entry)
            }
        } finally {
            release(openArchive)
        }
    }

    private fun acquire(path: Path): OpenArchive {
        synchronized(openArchives) {
            openArchives[path]?.let {
                it.referenceCount++
                return it
            }
        }
        val created = OpenArchive(path)
        val existing = synchronized(openArchives) {
            val existing = openArchives[path]
            if (existing != null) {
                existing.referenceCount++
            } else {
                // One reference of the cache and one of the reader.
                created.referenceCount = 2
                openArchives.put(path, created)
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

    private inner class OpenArchive(path: Path) {
        private val channel = FileChannel.open(path, StandardOpenOption.READ)

        val buffer = try {
            LargeDynamicMappedBuffer(
                channel.size(),
                { offset, size -> channel.map(FileChannel.MapMode.READ_ONLY, offset, size) },
                unmapBuffer,
                defaultByteOrder = ByteOrder.LITTLE_ENDIAN,
            )
        } catch (e: Throwable) {
            channel.close()
            throw e
        }

        /** Guarded by [openArchives]. */
        var referenceCount = 0

        fun close() {
            channel.close()
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
    // Plain standard linked hashmaps are used. An attempt to use sublassed map with a custom implementation of the `removeEldestEntry`
    // in place of the custom put methods below, led to noticeable performance degradation due to different JIT strategies, so the approach
    // was rejected.
    private val probationalQueue = LinkedHashMap<K, V>()
    private val protectedQueue = LinkedHashMap<K, V>()

    /** The latest entry of the [protectedQueue], if known: most of the requests are for the same key as the previous one. */
    private var latestKey: K? = null
    private var latestValue: V? = null

    operator fun get(key: K): V? {
        if (key == latestKey) return latestValue
        val value = protectedQueue.remove(key) ?: probationalQueue.remove(key) ?: return null
        putToProtected(key, value)
        return value
    }

    fun put(key: K, value: V) {
        forgetLatest(key)
        protectedQueue.remove(key)?.let(onDrop)
        probationalQueue.remove(key)?.let(onDrop)
        putToProbational(key, value)
    }

    fun clear() {
        latestKey = null
        latestValue = null
        protectedQueue.values.forEach(onDrop)
        probationalQueue.values.forEach(onDrop)
        protectedQueue.clear()
        probationalQueue.clear()
    }

    private fun putToProtected(key: K, value: V) {
        protectedQueue[key] = value
        latestKey = key
        latestValue = value
        if (protectedQueue.size <= protectedSize) return
        val eldest = protectedQueue.entries.iterator().next()
        val eldestKey = eldest.key
        val eldestValue = eldest.value
        forgetLatest(eldestKey)
        protectedQueue.remove(eldestKey)
        putToProbational(eldestKey, eldestValue)
    }

    private fun putToProbational(key: K, value: V) {
        probationalQueue[key] = value
        if (probationalQueue.size <= probationalSize) return
        val eldest = probationalQueue.entries.iterator().next()
        val eldestValue = eldest.value
        probationalQueue.remove(eldest.key)
        onDrop(eldestValue)
    }

    private fun forgetLatest(key: K) {
        if (key == latestKey) {
            latestKey = null
            latestValue = null
        }
    }
}
