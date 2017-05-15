/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.jvm.internal.cache

enum class CacheType {
    ARRAY,
    LRU,
    SYNCHRONIZED_LIST,
    RW_LIST
}

interface Cache<in Key, Value> {
    operator fun get(key: Key): Value?
    operator fun set(key: Key, value: Value)

    companion object {
        inline fun <reified Key, reified Value> makeCache(type: Int, size: Int): Cache<Key, Value> = when (CacheType.values()[type]) {
            CacheType.ARRAY -> SmallArrayCache(size)
            CacheType.LRU -> LRUCache(size)
            CacheType.SYNCHRONIZED_LIST -> SynchronizedListCache()
            CacheType.RW_LIST -> RwListCache()
        }
    }
}
