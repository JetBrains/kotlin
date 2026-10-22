/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Parsed metadata of Kotlin class files, shared between compilations in the same process.
 *
 * Each compilation creates new symbol providers, which would otherwise parse the metadata of the same libraries (like the stdlib) again.
 * Parsed protos and name resolvers are immutable and don't depend on the session, and the cache is keyed by the metadata content
 * (`d1` and `d2`), so a cached value is valid for any class file with the same metadata.
 *
 * Entries are softly reachable together with their keys, so the cache never prevents them from being collected.
 */
internal object JvmParsedMetadataCache {
    private val packages = SoftCache<Pair<JvmNameResolver, ProtoBuf.Package>>()
    private val classes = SoftCache<Pair<JvmNameResolver, ProtoBuf.Class>>()

    fun readPackageData(data: Array<String>, strings: Array<String>): Pair<JvmNameResolver, ProtoBuf.Package> =
        packages.getOrCompute(data, strings) { JvmProtoBufUtil.readPackageDataFrom(data, strings) }

    fun readClassData(data: Array<String>, strings: Array<String>): Pair<JvmNameResolver, ProtoBuf.Class> =
        classes.getOrCompute(data, strings) { JvmProtoBufUtil.readClassDataFrom(data, strings) }

    private class Entry<T : Any>(val data: Array<String>, val strings: Array<String>, val value: T)

    private class EntryReference<T : Any>(
        val hash: Int,
        entry: Entry<T>,
        queue: ReferenceQueue<Entry<T>>,
    ) : SoftReference<Entry<T>>(entry, queue)

    /**
     * Keeps a single entry per hash: an entry with the same hash but different content replaces the previous one, which is only a cache
     * miss.
     */
    private class SoftCache<T : Any> {
        private val queue = ReferenceQueue<Entry<T>>()
        private val references = ConcurrentHashMap<Int, EntryReference<T>>()

        inline fun getOrCompute(data: Array<String>, strings: Array<String>, compute: () -> T): T {
            removeClearedReferences()

            val hash = 31 * data.contentHashCode() + strings.contentHashCode()
            val entry = references[hash]?.get()
            if (entry != null && entry.data.contentEquals(data) && entry.strings.contentEquals(strings)) {
                return entry.value
            }

            return compute().also { references[hash] = EntryReference(hash, Entry(data, strings, it), queue) }
        }

        private fun removeClearedReferences() {
            while (true) {
                @Suppress("UNCHECKED_CAST")
                val reference = queue.poll() as? EntryReference<T> ?: return
                references.remove(reference.hash, reference)
            }
        }
    }
}
