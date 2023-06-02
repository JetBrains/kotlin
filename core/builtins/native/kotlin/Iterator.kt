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

package kotlin.collections

/**
 * An iterator over a collection or another entity that can be represented as a sequence of elements.
 * Allows to sequentially access the elements.
 */
public interface Iterator<out T> {
    /**
     * Returns the next element in the iteration.
     *
     * @throws NoSuchElementException if the iteration has no next element.
     */
    public operator fun next(): T

    /**
     * Returns `true` if the iteration has more elements.
     */
    public operator fun hasNext(): Boolean
}

/**
 * An iterator over a mutable collection. Provides the ability to remove elements while iterating.
 * @see MutableCollection.iterator
 */
public interface MutableIterator<out T> : Iterator<T> {
    /**
     * Removes from the underlying collection the last element returned by this iterator.
     *
     * @throws IllegalStateException if [next] has not been called yet,
     * or the most recent [next] call has already been followed by a [remove] call.
     */
    public fun remove(): Unit
}

/**
 * An iterator over a collection that supports indexed access.
 * @see List.listIterator
 */
public interface ListIterator<out T> : Iterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    /**
     * Returns `true` if there are elements in the iteration before the current element.
     */
    public fun hasPrevious(): Boolean

    /**
     * Returns the previous element in the iteration and moves the cursor position backwards.
     *
     * @throws NoSuchElementException if the iteration has no previous element.
     */
    public fun previous(): T

    /**
     * Returns the index of the element that would be returned by a subsequent call to [next].
     *
     * Returns collection size if the iteration is at the end of the collection.
     */
    public fun nextIndex(): Int

    /**
     * Returns the index of the element that would be returned by a subsequent call to [previous].
     *
     * Returns -1 if the iteration is at the beginning of the collection.
     */
    public fun previousIndex(): Int
}

/**
 * An iterator over a mutable collection that supports indexed access. Provides the ability
 * to add, modify and remove elements while iterating.
 * @see MutableList.listIterator
 */
public interface MutableListIterator<T> : ListIterator<T>, MutableIterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    // Modification Operations
    /**
     * Removes from the underlying collection the last element returned by this iterator.
     *
     * @throws IllegalStateException if neither [next] nor [previous] has not been called yet,
     * or the most recent [next] or [previous] call has already been followed by a [remove] or [add] call.
     */
    override fun remove(): Unit

    /**
     * Replaces the last element returned by [next] or [previous] with the specified element [element].
     *
     * @throws IllegalStateException if neither [next] nor [previous] has not been called yet,
     * or the most recent [next] or [previous] call has already been followed by a [remove] or [add] call.
     */
    public fun set(element: T): Unit

    /**
     * Adds the specified element [element] into the underlying collection immediately before the element that would be
     * returned by [next], if any, and after the element that would be returned by [previous], if any.
     * (If the collection contains no elements, the new element becomes the sole element in the collection.)
     * The new element is inserted before the implicit cursor: a subsequent call to [next] would be unaffected,
     * and a subsequent call to [previous] would return the new element. (This call increases by one the value \
     * that would be returned by a call to [nextIndex] or [previousIndex].)
     */
    public fun add(element: T): Unit
}
