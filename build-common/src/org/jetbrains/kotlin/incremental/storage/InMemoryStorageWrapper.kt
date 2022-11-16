/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

class InMemoryStorageWrapper<K, V>(val origin: LazyStorage<K, V>) : LazyStorage<K, V> {
    override val keys: Collection<K>
        get() = origin.keys

    override fun clean() {
        origin.clean()
    }

    override fun flush(memoryCachesOnly: Boolean) {
        origin.flush(memoryCachesOnly)
    }

    override fun close() {
        origin.close()
    }

    override fun append(key: K, value: V) {
        origin.append(key, value)
    }

    override fun remove(key: K) {
        origin.remove(key)
    }

    override fun set(key: K, value: V) {
        origin[key] = value
    }

    override fun get(key: K): V? = origin[key]

    override fun contains(key: K) = key in origin
}