/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.storage

import java.util.concurrent.locks.Lock
import java.util.ArrayList
import java.util.NoSuchElementException
import org.jetbrains.kotlin.util.printAndReturn

class MemoizingLazyIterable<T, D>(
        private val lock: Lock,
        // all access to this iterator is guarded by the lock
        private val data: Iterator<D>,
        // all calls to this function are guarded by the lock
        private val compute : (D) -> T
) : Iterable<T> {

    // this list contains results of applying compute() to items in data
    // data is traversed once, the size of this list reflects how many data items were seen
    private val storedValues = ArrayList<T>()

    private fun isElementAvailableAt(index: Int): Boolean {
        lock.lock()
        try {
            val size = storedValues.size()
            assert(index <= size) {"Trying to look too far ahead: $index, only ${size} elements are computed"}
            return index < size || data.hasNext()
        }
        finally {
            lock.unlock()
        }
    }

    private fun getByIndex(index: Int): T {
        lock.lock()
        try {
            if (storedValues.size() > index) return storedValues[index]

            for (i in storedValues.size()..index) {
                if (!data.hasNext()) throw NoSuchElementException("Index: $index")

                storedValues.add(compute(data.next()))
            }

            if (!data.hasNext()) storedValues.trimToSize()

            return storedValues[index]
        }
        finally {
            lock.unlock()
        }
    }

    // Accessing the same iterator from different threads is NOT safe
    // Having many iterators, each of which is accessed from only one thread, is safe
    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var index = 0

            override fun next() = getByIndex(index++)

            override fun hasNext() = isElementAvailableAt(index)
        }
    }

}