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

package kotlin

public trait Iterator<out T> {
    public fun next(): T
    public fun hasNext(): Boolean
}

public trait MutableIterator<out T> : Iterator<T> {
    public fun remove(): Unit
}

public trait ListIterator<out T> : Iterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    public fun hasPrevious(): Boolean
    public fun previous(): T
    public fun nextIndex(): Int
    public fun previousIndex(): Int
}

public trait MutableListIterator<T> : ListIterator<T>, MutableIterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    // Modification Operations
    override fun remove(): Unit
    public fun set(e: T): Unit
    public fun add(e: T): Unit
}
