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

class SmallArrayCache<in Key, Value>(val size: Int) : Cache<Key, Value> {
    private class Pair<out Key, out Value>(val key: Key, val value: Value)

    private val buffer = circularArrayOf<Pair<Key, Value>?>(size, { null })

    override operator fun get(key: Key): Value? {
        for (i in 0..size - 1) {
            val item = buffer[i] ?: return null

            if (item.key == key) {
                return item.value
            }
        }

        return null
    }

    override operator fun set(key: Key, value: Value) {
        buffer.shiftLeft(1)
        buffer[0] = Pair(key, value)
    }
}

private class CircularBuffer<T>(private val buffer: Array<T>) {
    private var start = 0

    val size: Int
        inline get() = buffer.size

    operator fun get(index: Int) = buffer[(index + start) % size]

    operator fun set(index: Int, value: T) {
        buffer[(index + start) % size] = value
    }

    fun shiftLeft(offset: Int) = rotate(offset)

    fun shiftRight(offset: Int) = rotate(-offset)

    private fun rotate(offset: Int) {
        start = if (start + offset < 0) {
            (size - (start + offset)) % size
        }
        else {
            (start + offset) % size
        }
    }
}

private inline fun <reified T> circularArrayOf(size: Int, noinline init: (Int) -> T): CircularBuffer<T> = CircularBuffer(Array(size, init))
