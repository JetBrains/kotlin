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

import java.io.Closeable

interface LazyStorage<K, V> : Closeable {
    val keys: Collection<K>
    operator fun contains(key: K): Boolean
    operator fun get(key: K): V?
    operator fun set(key: K, value: V)
    fun remove(key: K)
    fun clean()
    fun flush(memoryCachesOnly: Boolean)
    override fun close()
}

interface AppendableLazyStorage<K, V> : LazyStorage<K, V> {
    fun append(key: K, value: V)
}