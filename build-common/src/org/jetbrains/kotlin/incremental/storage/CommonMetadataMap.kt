/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.io.BooleanDataDescriptor
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.incremental.AbstractIncrementalCache
import org.jetbrains.kotlin.incremental.ChangesCollector
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.ProtoDataProvider
import org.jetbrains.kotlin.incremental.md5
import java.io.File

internal class CommonMetadataMap(
    storageFile: File,
    private val protoDataProvider: ProtoDataProvider,
    private val icContext: IncrementalCompilationContext,
) : BasicStringMap<Map<File, ByteArray>>(
    storageFile,
    MapExternalizer(icContext.fileDescriptorForSourceFiles, ByteArrayExternalizer),
    icContext,
) {
    operator fun get(key: String, file: File): ByteArray? = storage[key]?.get(file)

    @Synchronized
    fun putAndCollect(key: String, entries: Map<File, ByteArray>, changesCollector: ChangesCollector) {
        if (entries.isEmpty()) return

        val old = storage[key]
        for ([file, newBytes] in entries) {
            collectChanges(file, old?.get(file), newBytes, changesCollector)
        }

        val merged = old?.let { HashMap(it) } ?: HashMap(entries.size)
        merged.putAll(entries)

        storage[key] = merged

        for ([file, _] in entries) {
            icContext.reporter.debug { "[$key] Saved common metadata for file: ${file.path}" }
        }
    }

    @Synchronized
    fun remove(key: String, files: Collection<File>, changesCollector: ChangesCollector) {
        val inner = storage[key] ?: return

        val filesToRemove = files.filterTo(hashSetOf()) { it in inner }
        if (filesToRemove.isEmpty()) return

        for (file in filesToRemove) {
            collectChanges(file, inner.getValue(file), null, changesCollector)
        }

        val merged = HashMap(inner).apply { keys.removeAll(filesToRemove) }

        if (merged.isEmpty()) {
            storage.remove(key)
        } else {
            storage[key] = merged
        }

        for (file in filesToRemove) {
            icContext.reporter.debug { "[$key] Removed common metadata for file: ${file.path}" }
        }
    }

    private fun collectChanges(sourceFile: File, oldBytes: ByteArray?, newBytes: ByteArray?, changesCollector: ChangesCollector) {
        if (oldBytes != null && newBytes != null && oldBytes.contentEquals(newBytes)) return

        val oldProtoMap = oldBytes?.let { protoDataProvider(sourceFile, it) } ?: emptyMap()
        val newProtoMap = newBytes?.let { protoDataProvider(sourceFile, it) } ?: emptyMap()

        for (classId in oldProtoMap.keys + newProtoMap.keys) {
            changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
        }
    }

    override fun dumpValue(value: Map<File, ByteArray>): String =
        value.entries.sortedBy { it.key.path }
            .joinToString(", ", "{", "}") { "${it.key} -> ${java.lang.Long.toHexString(it.value.md5())}" }
}

/**
 * The source files whose entries in [CommonMetadataMap] are stale, in the same way [AbstractIncrementalCache.dirtyOutputClassesMap] tracks stale
 * classes.
 */
internal class DirtyCommonMetadataMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<File, Boolean>(storageFile, icContext.fileDescriptorForSourceFiles, BooleanDataDescriptor.INSTANCE, icContext) {
    @Synchronized
    fun markDirty(sourceFiles: Collection<File>) {
        for (sourceFile in sourceFiles) {
            this[sourceFile] = true
        }
    }

    @Synchronized
    fun notDirty(sourceFiles: Collection<File>) {
        for (sourceFile in sourceFiles) {
            remove(sourceFile)
        }
    }

    @Synchronized
    fun getDirtySources(): Set<File> = keys
}
