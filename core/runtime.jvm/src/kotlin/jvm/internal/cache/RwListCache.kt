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

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

class RwListCache<Key, Value> : Cache<Key, Value> {
    private val lock = ReentrantReadWriteLock()
    private val buffer = LinkedList<Pair<Key, Value>>()

    override fun set(key: Key, value: Value) {
        val writeLock = lock.writeLock()
        writeLock.lock()
        buffer.add(Pair(key, value))
        writeLock.unlock()
    }

    override fun get(key: Key): Value? {
        var result: Value? = null
        val readLock = lock.readLock()
        readLock.lock()

        for (item in buffer) {
            if (item.key == key) {
                result = item.value
                break
            }
        }

        readLock.unlock()
        return result
    }

    private class Pair<Key, Value>(val key: Key, val value: Value)
}
