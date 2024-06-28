/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.utils.ThreadSafe
import java.io.Closeable
import java.io.File

/**
 * Represents an in-memory map that is backed by a [storageFile].
 *
 * Changes to this map may be written to [storageFile] at any time, and it is guaranteed to be written on [flush] or [close].
 *
 * This interface is similar to but simpler than [com.intellij.util.io.PersistentMapBase].
 */
@Suppress("UnstableApiUsage")
interface PersistentStorage<KEY, VALUE> : Closeable {

    /** The storage file backing this map. */
    val storageFile: File

    val keys: Set<KEY>

    operator fun contains(key: KEY): Boolean

    operator fun get(key: KEY): VALUE?

    operator fun set(key: KEY, value: VALUE)

    fun remove(key: KEY)

    /** Writes any remaining in-memory changes to [storageFile]. */
    fun flush()

    /** Writes any remaining in-memory changes to [storageFile] ([flush]) and closes this map. */
    override fun close()
}

/** [PersistentStorage] where a map entry's value is a [Collection] of elements of type [E]. */
interface AppendablePersistentStorage<KEY, E> : PersistentStorage<KEY, Collection<E>> {

    /** Adds the given [elements] to the collection corresponding to the given [key]. */
    fun append(key: KEY, elements: Collection<E>)

    /** Adds the given [element] to the collection corresponding to the given [key]. */
    fun append(key: KEY, element: E) {
        append(key, listOf(element))
    }
}

/**
 * [PersistentStorage] that delegates operations to another [storage].
 *
 * THREAD SAFETY: All [PersistentStorage]s (both parent classes and their subclasses) need to be thread-safe. This requirement seems to come
 * from JPS -- see commit 275a02c; Gradle builds don't have this requirement. To ensure thread safety, currently all non-private
 * implementation methods of [PersistentStorage]s and their subclasses must be `@Synchronized`. A possibly better approach is to perform
 * synchronization in JPS, so that we don't have to provide the thread safety guarantee for [PersistentStorage]s.
 */
@ThreadSafe
abstract class PersistentStorageWrapper<KEY, VALUE>(
    private val storage: PersistentStorage<KEY, VALUE>,
) : PersistentStorage<KEY, VALUE> { // Can't use Kotlin delegation (`by storage`) here as we need to annotate the methods with @Synchronized

    override val storageFile: File = storage.storageFile

    @get:Synchronized
    override val keys: Set<KEY>
        get() = storage.keys

    @Synchronized
    override fun contains(key: KEY): Boolean =
        storage.contains(key)

    @Synchronized
    override fun get(key: KEY): VALUE? =
        storage[key]

    @Synchronized
    override fun set(key: KEY, value: VALUE) {
        storage[key] = value
    }

    @Synchronized
    override fun remove(key: KEY) {
        storage.remove(key)
    }

    @Synchronized
    override fun flush() {
        storage.flush()
    }

    @Synchronized
    override fun close() {
        storage.close()
    }
}

/** [PersistentStorageWrapper] where a map entry's value is a [Collection] of elements of type [E]. */
@ThreadSafe
abstract class AppendablePersistentStorageWrapper<KEY, E>(
    private val appendableStorage: AppendablePersistentStorage<KEY, E>,
) : PersistentStorageWrapper<KEY, Collection<E>>(appendableStorage), AppendablePersistentStorage<KEY, E> {

    @Synchronized
    override fun append(key: KEY, elements: Collection<E>) {
        appendableStorage.append(key, elements)
    }
}
