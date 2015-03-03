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

/**
 * Classes that inherit from this trait can be represented as a sequence of elements that can
 * be iterated over.
 * @param T the type of element being iterated over.
 */
public trait Iterable<out T> {
    /**
     * Returns an iterator over the elements of this object.
     */
    public fun iterator(): Iterator<T>
}

/**
 * Classes that inherit from this trait can be represented as a sequence of elements that can
 * be iterated over and that supports removing elements during iteration.
 */
public trait MutableIterable<out T> : Iterable<T> {
    /**
     * Returns an iterator over the elementrs of this sequence that supports removing elements during iteration.
     */
    override fun iterator(): MutableIterator<T>
}

/**
 * A generic collection of elements. Methods in this trait support only read-only access to the collection;
 * read/write access is supported through the [MutableCollection] trait.
 * @param E the type of elements contained in the collection.
 */
public trait Collection<out E> : Iterable<E> {
    // Query Operations
    /**
     * Returns the size of the collection.
     */
    public fun size(): Int

    /**
     * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
     */
    public fun isEmpty(): Boolean

    /**
     * Checks if the specified element is contained in this collection.
     */
    public fun contains(o: Any?): Boolean
    override fun iterator(): Iterator<E>

    // Bulk Operations
    /**
     * Checks if all elements in the specified collection are contained in this collection.
     */
    public fun containsAll(c: Collection<Any?>): Boolean
}

/**
 * A generic collection of elements that supports adding and removing elements.
 */
public trait MutableCollection<E> : Collection<E>, MutableIterable<E> {
    // Query Operations
    override fun iterator(): MutableIterator<E>

    // Modification Operations
    /**
     * Adds the specified element to the collection.
     *
     * @return `true` if the element has been added, `false` if the collection does not support duplicates
     * and the element is already contained in the collection.
     */
    public fun add(e: E): Boolean

    /**
     * Removes the specified element from the collection.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    public fun remove(o: Any?): Boolean

    // Bulk Modification Operations
    /**
     * Adds all of the elements in the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    public fun addAll(c: Collection<E>): Boolean

    /**
     * Removes all of the elements in the specified collection from this collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     */
    public fun removeAll(c: Collection<Any?>): Boolean

    /**
     * Removes all of the elements not contained in the specified collection from this collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     */
    public fun retainAll(c: Collection<Any?>): Boolean

    /**
     * Removes all elements from this collection.
     */
    public fun clear(): Unit
}

/**
 * A generic ordered collection of elements. Methods in this trait support only read-only access to the list;
 * read/write access is supported through the [MutableList] trait.
 * @param E the type of elements contained in the list.
 */
public trait List<out E> : Collection<E> {
    // Query Operations
    override fun size(): Int
    override fun isEmpty(): Boolean
    override fun contains(o: Any?): Boolean
    override fun iterator(): Iterator<E>

    // Bulk Operations
    override fun containsAll(c: Collection<Any?>): Boolean

    // Positional Access Operations
    /**
     * Returns the element at the specified index in the list.
     */
    public fun get(index: Int): E

    // Search Operations
    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public fun indexOf(o: Any?): Int

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public fun lastIndexOf(o: Any?): Int

    // List Iterators
    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     */
    public fun listIterator(): ListIterator<E>

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     */
    public fun listIterator(index: Int): ListIterator<E>

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
     */
    public fun subList(fromIndex: Int, toIndex: Int): List<E>
}

/**
 * A generic ordered collection of elements that supports adding and removing elements.
 * @param E the type of elements contained in the list.
 */
public trait MutableList<E> : List<E>, MutableCollection<E> {
    // Modification Operations
    override fun add(e: E): Boolean
    override fun remove(o: Any?): Boolean

    // Bulk Modification Operations
    override fun addAll(c: Collection<E>): Boolean

    /**
     * Inserts all of the elements in the specified collection [c] into this list at the specified [index].
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    public fun addAll(index: Int, c: Collection<E>): Boolean
    override fun removeAll(c: Collection<Any?>): Boolean
    override fun retainAll(c: Collection<Any?>): Boolean
    override fun clear(): Unit

    // Positional Access Operations
    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @return the element previously at the specified position.
     */
    public fun set(index: Int, element: E): E

    /**
     * Inserts an element into the list at the specified [index].
     */
    public fun add(index: Int, element: E): Unit

    /**
     * Removes an element at the specified [index] from the list.
     *
     * @return the element that has been removed.
     */
    public fun remove(index: Int): E

    // List Iterators
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>

    // View
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

/**
 * A generic unordered collection of elements that does not support duplicate elements.
 * Methods in this trait support only read-only access to the set;
 * read/write access is supported through the [MutableSet] trait.
 * @param E the type of elements contained in the set.
 */
public trait Set<out E> : Collection<E> {
    // Query Operations
    override fun size(): Int
    override fun isEmpty(): Boolean
    override fun contains(o: Any?): Boolean
    override fun iterator(): Iterator<E>

    // Bulk Operations
    override fun containsAll(c: Collection<Any?>): Boolean
}

/**
 * A generic unordered collection of elements that does not support duplicate elements, and supports
 * adding and removing elements.
 * @param E the type of elements contained in the set.
 */
public trait MutableSet<E> : Set<E>, MutableCollection<E> {
    // Query Operations
    override fun iterator(): MutableIterator<E>

    // Modification Operations
    override fun add(e: E): Boolean
    override fun remove(o: Any?): Boolean

    // Bulk Modification Operations
    override fun addAll(c: Collection<E>): Boolean
    override fun removeAll(c: Collection<Any?>): Boolean
    override fun retainAll(c: Collection<Any?>): Boolean
    override fun clear(): Unit
}

/**
 * A collection that holds pairs of objects (keys and values) and supports efficiently retrieving
 * the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
 * Methods in this trait support only read-only access to the map; read-write access is supported through
 * the [MutableMap] trait.
 * @param K the type of map keys.
 * @param V the type of map values.
 */
public trait Map<K, out V> {
    // Query Operations
    /**
     * Returns the number of key/value pairs in the map.
     */
    public fun size(): Int

    /**
     * Returns `true` if the map is empty (contains no elements), `false` otherwise.
     */
    public fun isEmpty(): Boolean

    /**
     * Returns `true` if the map contains the specified [key].
     */
    public fun containsKey(key: Any?): Boolean

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     */
    public fun containsValue(value: Any?): Boolean

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     */
    public fun get(key: Any?): V?

    // Views
    /**
     * Returns a [Set] of all keys in this map.
     */
    public fun keySet(): Set<K>

    /**
     * Returns a [Collection] of all values in this map. Note that this collection may contain duplicate values.
     */
    public fun values(): Collection<V>

    /**
     * Returns a [Set] of all key/value pairs in this map.
     */
    public fun entrySet(): Set<Map.Entry<K, V>>

    /**
     * Represents a key/value pair held by a [Map].
     */
    public trait Entry<out K, out V> {
        /**
         * Returns the key of this key/value pair.
         */
        public fun getKey(): K

        /**
         * Returns the value of this key/value pair.
         */
        public fun getValue(): V
    }
}

/**
 * A modifiable collection that holds pairs of objects (keys and values) and supports efficiently retrieving
 * the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
 * @param K the type of map keys.
 * @param V the type of map values.
 */
public trait MutableMap<K, V> : Map<K, V> {
    // Modification Operations
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    public fun put(key: K, value: V): V?

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    public fun remove(key: Any?): V?

    // Bulk Modification Operations
    /**
     * Updates this map with key/value pairs from the specified map [m].
     */
    public fun putAll(m: Map<out K, V>): Unit

    /**
     * Removes all elements from this map.
     */
    public fun clear(): Unit

    // Views
    override fun keySet(): MutableSet<K>
    override fun values(): MutableCollection<V>
    override fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>>

    /**
     * Represents a key/value pair held by a [MutableMap].
     */
    public trait MutableEntry<K,V>: Map.Entry<K, V> {
        /**
         * Changes the value associated with the key of this entry.
         *
         * @return the previous value corresponding to the key.
         */
    	public fun setValue(value: V): V
    }
}
