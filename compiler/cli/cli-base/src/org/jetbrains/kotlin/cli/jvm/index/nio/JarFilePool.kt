/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.LargeDynamicMappedBuffer
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.ZipEntryDescription
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.contentsToByteArray
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.parseCentralDirectory
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.prepareCleanerCallback
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * The jar archives read by NIO root entries ([JarRootEntry]), with the reader of FastJar but without its VFS layer.
 *
 * The central directory of each archive is read once, on first use, into an immutable entry tree. Reading an entry maps the archive;
 * the mapped archives are kept open, at most [MAX_OPEN_FILES] at a time, until [close]. Closing releases (unmaps and closes) the open
 * archives; it is idempotent, and a later read opens its archive again, so that handles stay usable after the owner released the
 * resources at an environment boundary.
 *
 * The pool is owned by the classpath indexes of a compiler environment, not by the process. It is thread-safe.
 */
class JarFilePool private constructor(private val unmapBuffer: MappedByteBuffer.() -> Unit) : Closeable {
    private val rootEntries = HashMap<Path, JarRootEntry>()

    private val openFiles = object : LinkedHashMap<Path, OpenFile>(MAX_OPEN_FILES, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Path, OpenFile>): Boolean {
            if (size <= MAX_OPEN_FILES) return false
            eldest.value.close()
            return true
        }
    }

    /** The root entry of the jar at [jar]; an unreadable archive has no entries, as in FastJar. */
    fun rootEntry(jar: Path): JarRootEntry = synchronized(rootEntries) {
        rootEntries.getOrPut(jar) { JarRootEntry.build(this, jar, readCentralDirectory(jar)) }
    }

    private fun readCentralDirectory(jar: Path): List<ZipEntryDescription> =
        try {
            RandomAccessFile(jar.toFile(), "r").use { file ->
                val buffer = mappedBuffer(file)
                try {
                    buffer.parseCentralDirectory()
                } finally {
                    buffer.unmap()
                }
            }
        } catch (e: Exception) {
            Logger.getInstance(JarFilePool::class.java).warn("Error while reading zip file: $jar: $e", e)
            emptyList()
        }

    internal fun readBytes(jar: Path, entry: ZipEntryDescription): ByteArray {
        val openFile = synchronized(openFiles) {
            openFiles.getOrPut(jar) {
                val file = RandomAccessFile(jar.toFile(), "r")
                OpenFile(jar, file, mappedBuffer(file)).also { openArchives.merge(jar, 1, Int::plus) }
            }.also { it.users++ }
        }
        try {
            return synchronized(openFile) { openFile.buffer.contentsToByteArray(entry) }
        } finally {
            synchronized(openFiles) {
                openFile.users--
                if (openFile.isReleased && openFile.users == 0) openFile.closeNow()
            }
        }
    }

    /** The number of currently open archives, for tests. */
    val openFileCount: Int
        get() = synchronized(openFiles) { openFiles.size }

    override fun close() {
        synchronized(openFiles) {
            openFiles.values.forEach(OpenFile::close)
            openFiles.clear()
        }
    }

    private fun mappedBuffer(file: RandomAccessFile): LargeDynamicMappedBuffer =
        LargeDynamicMappedBuffer(
            file.length(),
            { offset, size -> file.channel.map(FileChannel.MapMode.READ_ONLY, offset, size) },
            unmapBuffer,
            defaultByteOrder = ByteOrder.LITTLE_ENDIAN,
        )

    /**
     * An open archive; guarded by the lock of [openFiles], except for reading [buffer], which is guarded by the instance.
     * Lock order: the lock of [openFiles] is taken before the instance lock.
     */
    private class OpenFile(val jar: Path, val file: RandomAccessFile, val buffer: LargeDynamicMappedBuffer) {
        var users = 0
        var isReleased = false
        private var isClosed = false

        /** Closes the archive now, or after the last running read if it is in use. */
        fun close() {
            isReleased = true
            if (users == 0) closeNow()
        }

        fun closeNow() {
            synchronized(this) {
                if (isClosed) return
                isClosed = true
                buffer.unmap()
                file.close()
                openArchives.computeIfPresent(jar) { _, count -> (count - 1).takeIf { it > 0 } }
            }
        }
    }

    companion object {
        private const val MAX_OPEN_FILES = 20

        /** The number of pools which have each archive open, over all pools of the process. */
        private val openArchives = ConcurrentHashMap<Path, Int>()

        /** Whether any pool of the process has the archive [jar] open; for leak checks in tests. */
        fun isOpenInAnyPool(jar: Path): Boolean = openArchives.containsKey(jar)

        /** Creates a pool, or returns `null` if mapped buffers cannot be unmapped on this JVM (then FastJar is not used either). */
        fun createIfUnmappingPossible(): JarFilePool? = prepareCleanerCallback()?.let { callback -> JarFilePool { callback(this) } }
    }
}
