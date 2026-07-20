/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

sealed class ArrayMap<T : Any> : Iterable<T> {
    abstract val size: Int

    abstract operator fun set(index: Int, value: T)
    abstract operator fun get(index: Int): T?

    abstract fun copy(): ArrayMap<T>
}

internal object EmptyArrayMap : ArrayMap<Nothing>() {
    override val size: Int
        get() = 0

    override fun set(index: Int, value: Nothing) {
        throw IllegalStateException()
    }

    override fun get(index: Int): Nothing? {
        return null
    }

    override fun copy(): ArrayMap<Nothing> = this

    override fun iterator(): Iterator<Nothing> {
        return object : Iterator<Nothing> {
            override fun hasNext(): Boolean = false

            override fun next(): Nothing = throw NoSuchElementException()
        }
    }
}

internal class OneElementArrayMap<T : Any>(val value: T, val index: Int) : ArrayMap<T>() {
    override val size: Int
        get() = 1

    override fun set(index: Int, value: T) {
        throw IllegalStateException()
    }

    override fun get(index: Int): T? {
        return if (index == this.index) value else null
    }

    override fun copy(): ArrayMap<T> = OneElementArrayMap(value, index)

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var notVisited = true

            override fun hasNext(): Boolean {
                return notVisited
            }

            override fun next(): T {
                if (notVisited) {
                    notVisited = false
                    return value
                } else {
                    throw NoSuchElementException()
                }
            }
        }
    }
}

internal class ArrayMapImpl<T : Any> private constructor(
    private var data: Array<Any?>,
    initialSize: Int
) : ArrayMap<T>() {
    companion object {
        private const val DEFAULT_SIZE = 20
        private const val INCREASE_K = 2
    }

    constructor() : this(arrayOfNulls<Any>(DEFAULT_SIZE), 0)

    override var size: Int = initialSize
        private set


    private fun ensureCapacity(index: Int) {
        if (data.size > index) return
        var newSize = data.size
        do {
            newSize *= INCREASE_K
        } while (newSize <= index)
        data = data.copyOf(newSize)
    }

    override operator fun set(index: Int, value: T) {
        ensureCapacity(index)
        if (data[index] == null) {
            size++
        }
        data[index] = value
    }

    override operator fun get(index: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return data.getOrNull(index) as T?
    }

    override fun copy(): ArrayMap<T> = ArrayMapImpl(data.copyOf(), size)

    override fun iterator(): Iterator<T> {
        return object : AbstractIterator<T>() {
            private var index = -1

            override fun computeNext() {
                do {
                    index++
                } while (index < data.size && data[index] == null)
                if (index >= data.size) {
                    done()
                } else {
                    @Suppress("UNCHECKED_CAST")
                    setNext(data[index] as T)
                }
            }
        }
    }

    fun remove(index: Int) {
        if (data[index] != null) {
            size--
        }
        data[index] = null
    }

    fun entries(): List<Entry<T>> {
        @Suppress("UNCHECKED_CAST")
        return data.mapIndexedNotNull { index, value -> if (value != null) Entry(index, value as T) else null }
    }

    data class Entry<T>(override val key: Int, override val value: T) : Map.Entry<Int, T>
}

/**
 * An [ArrayMap] for 2 to 4 entries backed by four inlined `index`/`value` field pairs instead of a sparse array.
 * Unoccupied slots are sentineled with an index of [EMPTY], which is safe because `TypeRegistry` ids are always
 * non-negative.
 *
 * Compared to [ArrayMapImpl] — whose backing array is sized by the maximum key *index* and is therefore wasteful for
 * the small, sparsely-keyed maps typical of FIR declaration and cone-type attributes — this occupies a flat ~48 bytes
 * with no backing array. See KT-87817.
 *
 * The map is immutable: [set] throws, and [AttributeArrayOwner] mutates by publishing a fresh instance via
 * [withAddedOrReplaced]/[withRemoved]. The `val` fields give safe publication for lock-free reads (matching
 * [OneElementArrayMap]), so it can also be shared instead of copied.
 *
 * Slots are always filled front-to-back with no gaps, so [size] is determined by the topmost occupied slot.
 */
//internal class TinyArrayMap<T : Any> private constructor(
//    private val index0: Int,
//    private val value0: T?,
//    private val index1: Int,
//    private val value1: T?,
//    private val index2: Int,
//    private val value2: T?,
//    private val index3: Int,
//    private val value3: T?,
//) : ArrayMap<T>() {
//    companion object {
//        private const val EMPTY = -1
//    }
//
//    /** Creates a two-element map. Used when a [OneElementArrayMap] grows. */
//    constructor(index0: Int, value0: T, index1: Int, value1: T) :
//            this(index0, value0, index1, value1, EMPTY, null, EMPTY, null)
//
//    override val size: Int
//        get() = when {
//            // Note: We can only use this "check trailing index" approach when entries are packed into the front slots. There should be no
//            // gaps. Introducing a `size` field would increase the memory footprint of `TinyArrayMap`.
//            index3 != EMPTY -> 4
//            index2 != EMPTY -> 3
//            index1 != EMPTY -> 2
//            index0 != EMPTY -> 1
//            else -> 0
//        }
//
//    override fun set(index: Int, value: T) {
//        throw IllegalStateException("Cannot set a value. `${TinyArrayMap::class.simpleName}` is immutable.")
//    }
//
//    override fun get(index: Int): T? {
//        if (index == index0) return value0
//        if (index == index1) return value1
//        if (index == index2) return value2
//        if (index == index3) return value3
//        return null
//    }
//
//    // The map is immutable, so it can be shared instead of copied.
//    override fun copy(): ArrayMap<T> = this
//
//    override fun iterator(): Iterator<T> {
//        return object : AbstractIterator<T>() {
//            private var currentIndex = 0
//
//            override fun computeNext() {
//                when (currentIndex) {
//                    0 -> if (index0 != EMPTY) setNext(value0!!) else done()
//                    1 -> if (index1 != EMPTY) setNext(value1!!) else done()
//                    2 -> if (index2 != EMPTY) setNext(value2!!) else done()
//                    3 -> if (index3 != EMPTY) setNext(value3!!) else done()
//                    else -> done()
//                }
//                currentIndex += 1
//            }
//        }
//    }
//
//    /**
//     * Returns a copy with [index]/[value] replacing an existing entry or appended to a free slot. When the map is
//     * full (four entries) and [index] is new, the entries spill into an [ArrayMapImpl].
//     */
//    fun withAddedOrReplaced(index: Int, value: T): ArrayMap<T> {
//        // Replace an existing entry.
//        if (index == index0) return TinyArrayMap(index, value, index1, value1, index2, value2, index3, value3)
//        if (index == index1) return TinyArrayMap(index0, value0, index, value, index2, value2, index3, value3)
//        if (index == index2) return TinyArrayMap(index0, value0, index1, value1, index, value, index3, value3)
//        if (index == index3) return TinyArrayMap(index0, value0, index1, value1, index2, value2, index, value)
//
//        // Append to the first free slot, or spill into an `ArrayMapImpl` when full.
//        return when {
//            index2 == EMPTY -> TinyArrayMap(index0, value0, index1, value1, index, value, EMPTY, null)
//            index3 == EMPTY -> TinyArrayMap(index0, value0, index1, value1, index2, value2, index, value)
//            else -> ArrayMapImpl<T>().apply {
//                set(index0, value0!!)
//                set(index1, value1!!)
//                set(index2, value2!!)
//                set(index3, value3!!)
//                set(index, value)
//            }
//        }
//    }
//
//    /**
//     * Returns a copy with the entry at [index] removed, compacting the surviving entries front-to-back. Downgrades to
//     * a [OneElementArrayMap] when a single entry remains. [index] is expected to be present.
//     */
//    fun withRemoved(index: Int): ArrayMap<T> {
//        var survivingIndex0 = EMPTY
//        var survivingValue0: T? = null
//        var survivingIndex1 = EMPTY
//        var survivingValue1: T? = null
//        var survivingIndex2 = EMPTY
//        var survivingValue2: T? = null
//        var count = 0
//
//        fun keep(slotIndex: Int, slotValue: T?) {
//            if (slotIndex == EMPTY || slotIndex == index) return
//            when (count) {
//                0 -> {
//                    survivingIndex0 = slotIndex; survivingValue0 = slotValue
//                }
//                1 -> {
//                    survivingIndex1 = slotIndex; survivingValue1 = slotValue
//                }
//                2 -> {
//                    survivingIndex2 = slotIndex; survivingValue2 = slotValue
//                }
//            }
//            count++
//        }
//
//        keep(index0, value0)
//        keep(index1, value1)
//        keep(index2, value2)
//        keep(index3, value3)
//
//        return if (count == 1) {
//            OneElementArrayMap(survivingValue0!!, survivingIndex0)
//        } else {
//            TinyArrayMap(
//                survivingIndex0, survivingValue0,
//                survivingIndex1, survivingValue1,
//                survivingIndex2, survivingValue2,
//                EMPTY, null,
//            )
//        }
//    }
//}

/**
 * An alternative [TinyArrayMap] that does **not** maintain the "entries are packed into the front slots" invariant:
 * a removal simply clears its slot, leaving a hole. This trades the O(1) trailing-index [size] for a slot count, but
 * makes the [withAddedOrReplaced]/[withRemoved] transitions uniform — each computes the target slot, then multiplexes
 * a single copy over that slot.
 *
 * Otherwise identical to [TinyArrayMap]: immutable (`val` fields, [set] throws), ~48 bytes, `EMPTY` (-1) sentinel for
 * unoccupied slots, `copy()` shares `this`.
 */
internal class TinyArrayMap<T : Any> private constructor(
    private val index0: Int,
    private val value0: T?,
    private val index1: Int,
    private val value1: T?,
    private val index2: Int,
    private val value2: T?,
    private val index3: Int,
    private val value3: T?,
) : ArrayMap<T>() {
    companion object {
        private const val EMPTY = -1
        private const val NO_SLOT = -1
    }

    /** Creates a two-element map. Used when a [OneElementArrayMap] grows. */
    constructor(index0: Int, value0: T, index1: Int, value1: T) :
            this(index0, value0, index1, value1, EMPTY, null, EMPTY, null)

    override val size: Int
        get() {
            // Slots may contain gaps, so the size must count every occupied slot rather than reading the top one.
            var count = 0
            if (index0 != EMPTY) count++
            if (index1 != EMPTY) count++
            if (index2 != EMPTY) count++
            if (index3 != EMPTY) count++
            return count
        }

    override fun set(index: Int, value: T) {
        throw IllegalStateException("Cannot set a value. `${TinyArrayMap::class.simpleName}` is immutable.")
    }

    override fun get(index: Int): T? {
        if (index == index0) return value0
        if (index == index1) return value1
        if (index == index2) return value2
        if (index == index3) return value3
        return null
    }

    // The map is immutable, so it can be shared instead of copied.
    override fun copy(): ArrayMap<T> = this

    override fun iterator(): Iterator<T> {
        return object : AbstractIterator<T>() {
            private var currentSlot = 0

            override fun computeNext() {
                // Skip empty slots; unlike TinyArrayMap the occupied slots are not necessarily contiguous.
                while (true) {
                    val slot = currentSlot
                    currentSlot += 1
                    when (slot) {
                        0 -> if (index0 != EMPTY) return setNext(value0!!)
                        1 -> if (index1 != EMPTY) return setNext(value1!!)
                        2 -> if (index2 != EMPTY) return setNext(value2!!)
                        3 -> if (index3 != EMPTY) return setNext(value3!!)
                        else -> return done()
                    }
                }
            }
        }
    }

    /**
     * Returns a copy with [index]/[value] replacing an existing entry or written to the first free slot. When the map
     * is full (four entries) and [index] is new, the entries spill into an [ArrayMapImpl].
     */
    fun withAddedOrReplaced(index: Int, value: T): ArrayMap<T> {
        val slot = slotForAddOrReplace(index)
        if (slot == NO_SLOT) {
            // Full and `index` is new: spill into an ArrayMapImpl.
            return ArrayMapImpl<T>().apply {
                set(index0, value0!!)
                set(index1, value1!!)
                set(index2, value2!!)
                set(index3, value3!!)
                set(index, value)
            }
        }
        return copyWithSlot(slot, index, value)
    }

    /**
     * Returns a copy with the entry at [index] removed (its slot cleared). Downgrades to a [OneElementArrayMap] when a
     * single entry remains. [index] is expected to be present.
     */
    fun withRemoved(index: Int): ArrayMap<T> {
        val slot = slotOf(index)
        if (slot == NO_SLOT) return this
        val removed = copyWithSlot(slot, EMPTY, null)
        return if (removed.size == 1) removed.toOneElementArrayMap() else removed
    }

    /** The slot already holding [index] (replace), else the first free slot (append), else [NO_SLOT] (full). */
    private fun slotForAddOrReplace(index: Int): Int {
        if (index == index0) return 0
        if (index == index1) return 1
        if (index == index2) return 2
        if (index == index3) return 3
        if (index0 == EMPTY) return 0
        if (index1 == EMPTY) return 1
        if (index2 == EMPTY) return 2
        if (index3 == EMPTY) return 3
        return NO_SLOT
    }

    /** The slot holding [index], or [NO_SLOT] if absent. */
    private fun slotOf(index: Int): Int {
        if (index == index0) return 0
        if (index == index1) return 1
        if (index == index2) return 2
        if (index == index3) return 3
        return NO_SLOT
    }

    /** Returns a copy with [slot] set to [index]/[value] (pass [EMPTY]/`null` to clear the slot). */
    private fun copyWithSlot(slot: Int, index: Int, value: T?): TinyArrayMap<T> = when (slot) {
        0 -> TinyArrayMap(index, value, index1, value1, index2, value2, index3, value3)
        1 -> TinyArrayMap(index0, value0, index, value, index2, value2, index3, value3)
        2 -> TinyArrayMap(index0, value0, index1, value1, index, value, index3, value3)
        else -> TinyArrayMap(index0, value0, index1, value1, index2, value2, index, value) // slot == 3
    }

    private fun toOneElementArrayMap(): OneElementArrayMap<T> {
        if (index0 != EMPTY) return OneElementArrayMap(value0!!, index0)
        if (index1 != EMPTY) return OneElementArrayMap(value1!!, index1)
        if (index2 != EMPTY) return OneElementArrayMap(value2!!, index2)
        return OneElementArrayMap(value3!!, index3)
    }
}
