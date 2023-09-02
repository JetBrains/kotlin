/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlin.collections

private const val INT_MAX_POWER_OF_TWO: Int = 1 shl (Int.SIZE_BITS - 2)

@PublishedApi
internal fun mapCapacity1(expectedSize: Int): Int = when {
    // We are not coercing the value to a valid one and not throwing an exception. It is up to the caller to
    // properly handle negative values.
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    expectedSize < INT_MAX_POWER_OF_TWO -> ((expectedSize / 0.75F) + 1.0F).toInt()
    // any large value
    else -> Int.MAX_VALUE
}

@PublishedApi
internal fun <T> Iterable<T>.collectionSizeOrDefault1(default: Int): Int = if (this is Collection<*>) this.size else default

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun <T> Array<out T>.slice(indices: Collection<Int>): List<T> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<T>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun ByteArray.slice(indices: Collection<Int>): List<Byte> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Byte>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun ShortArray.slice(indices: Collection<Int>): List<Short> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Short>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun IntArray.slice(indices: Collection<Int>): List<Int> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Int>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun LongArray.slice(indices: Collection<Int>): List<Long> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Long>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun FloatArray.slice(indices: Collection<Int>): List<Float> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Float>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun DoubleArray.slice(indices: Collection<Int>): List<Double> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Double>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun BooleanArray.slice(indices: Collection<Int>): List<Boolean> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Boolean>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun CharArray.slice(indices: Collection<Int>): List<Char> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<Char>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
public fun <T> List<T>.slice(indices: Collection<Int>): List<T> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<T>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 */
@SinceKotlin("1.9")
public fun <T> Collection<T>.sortedWith1(comparator: Comparator<in T>): List<T> {
    if (size <= 1) return this.toList()
    @Suppress("UNCHECKED_CAST")
    return (toTypedArray<Any?>() as Array<T>).apply { sortWith(comparator) }.asList()
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given collection.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 *
 * @sample samples.collections.Collections.Transformations.associate
 */
@SinceKotlin("1.9")
public inline fun <T, K, V> Collection<T>.associate(transform: (T) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity1(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap(capacity), transform)
}

/**
 * Returns a [Map] containing the elements from the given collection indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 *
 * @sample samples.collections.Collections.Transformations.associateBy
 */
@SinceKotlin("1.9")
public inline fun <T, K> Collection<T>.associateBy(keySelector: (T) -> K): Map<K, T> {
    val capacity = mapCapacity1(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap(capacity), keySelector)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given collection.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 *
 * @sample samples.collections.Collections.Transformations.associateByWithValueTransform
 */
@SinceKotlin("1.9")
public inline fun <T, K, V> Collection<T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, V> {
    val capacity = mapCapacity1(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] where keys are elements from the given collection and values are
 * produced by the [valueSelector] function applied to each element.
 *
 * If any two elements are equal, the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 *
 * @sample samples.collections.Collections.Transformations.associateWith
 */
@SinceKotlin("1.9")
public inline fun <K, V> Collection<K>.associateWith(valueSelector: (K) -> V): Map<K, V> {
    val result = LinkedHashMap<K, V>(mapCapacity1(size).coerceAtLeast(16))
    return associateWithTo(result, valueSelector)
}

/**
 * Returns a new [HashSet] of all elements.
 */
@SinceKotlin("1.9")
public fun <T> Collection<T>.toHashSet(): HashSet<T> = toCollection(HashSet(mapCapacity1(size)))

/**
 * Returns a [List] containing all elements.
 */
@SinceKotlin("1.9")
public fun <T> Collection<T>.toList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> listOf(if (this is List) this[0] else iterator().next())
    else -> this.toMutableList()
}

/**
 * Returns a [List] containing all elements.
 */
@SinceKotlin("1.9")
public fun <T> List<T>.toList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> listOf(this[0])
    else -> this.toMutableList()
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
@SinceKotlin("1.9")
public inline fun <T, R> Collection<T>.map(transform: (T) -> R): List<R> = mapTo(ArrayList(size), transform)

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original collection.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
@SinceKotlin("1.9")
public inline fun <T, R> Collection<T>.mapIndexed(transform: (index: Int, T) -> R): List<R> = mapIndexedTo(ArrayList(size), transform)

/**
 * Returns `true` if all elements match the given [predicate].
 *
 * Note that if the collection contains no elements, the function returns `true`
 * because there are no elements in it that _do not_ match the predicate.
 * See a more detailed explanation of this logic concept in ["Vacuous truth"](https://en.wikipedia.org/wiki/Vacuous_truth) article.
 *
 * @sample samples.collections.Collections.Aggregates.all
 */
@SinceKotlin("1.9")
public inline fun <T> Collection<T>.all(predicate: (T) -> Boolean): Boolean {
    if (isEmpty()) return true
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if collection has at least one element.
 *
 * @sample samples.collections.Collections.Aggregates.any
 */
@SinceKotlin("1.9")
public fun <T> Collection<T>.any(): Boolean = isNotEmpty()

/**k
 * Returns `true` if at least one element matches the given [predicate].
 *
 * @sample samples.collections.Collections.Aggregates.anyWithPredicate
 */
@SinceKotlin("1.9")
public inline fun <T> Collection<T>.any(predicate: (T) -> Boolean): Boolean {
    if (isEmpty()) return false
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements matching the given [predicate].
 */
@SinceKotlin("1.9")
public inline fun <T> Collection<T>.count(predicate: (T) -> Boolean): Int {
    if (isEmpty()) return 0
    var count = 0
    for (element in this) if (predicate(element)) ++count
    return count
}

/**
 * Returns `true` if the collection has no elements.
 *
 * @sample samples.collections.Collections.Aggregates.none
 */
@SinceKotlin("1.9")
public fun <T> Collection<T>.none(): Boolean {
    return isEmpty()
}


/**
 * Returns a list containing successive accumulation values generated by applying [operation] from left to right
 * to each element and current accumulator value that starts with [initial] value.
 *
 * Note that `acc` value passed to [operation] function should not be mutated;
 * otherwise it would affect the previous value in resulting list.
 *
 * @param [operation] function that takes current accumulator value and an element, and calculates the next accumulator value.
 *
 * @sample samples.collections.Collections.Aggregates.runningFold
 */
@SinceKotlin("1.9")
public fun <T, R> Collection<T>.runningFold(initial: R, operation: (acc: R, T) -> R): List<R> {
    val estimatedSize = size
    if (estimatedSize == 0) return listOf(initial)
    val result = ArrayList<R>(estimatedSize + 1).apply { add(initial) }
    var accumulator = initial
    for (element in this) {
        accumulator = operation(accumulator, element)
        result.add(accumulator)
    }
    return result
}

/**
 * Returns a list containing successive accumulation values generated by applying [operation] from left to right
 * to each element, its index in the original collection and current accumulator value that starts with [initial] value.
 *
 * Note that `acc` value passed to [operation] function should not be mutated;
 * otherwise it would affect the previous value in resulting list.
 *
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 *
 * @sample samples.collections.Collections.Aggregates.runningFold
 */
@SinceKotlin("1.9")
public fun <T, R> Collection<T>.runningFoldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R): List<R> {
    val estimatedSize = size
    if (estimatedSize == 0) return listOf(initial)
    val result = ArrayList<R>(estimatedSize + 1).apply { add(initial) }
    var index = 0
    var accumulator = initial
    for (element in this) {
        accumulator = operation(index++, accumulator, element)
        result.add(accumulator)
    }
    return result
}


/**
 * Returns a list containing successive accumulation values generated by applying [operation] from left to right
 * to each element and current accumulator value that starts with the first element of this collection.
 *
 * Note that `acc` value passed to [operation] function should not be mutated;
 * otherwise it would affect the previous value in resulting list.
 *
 * @param [operation] function that takes current accumulator value and the element, and calculates the next accumulator value.
 *
 * @sample samples.collections.Collections.Aggregates.runningReduce
 */
@SinceKotlin("1.9")
public fun <S, T : S> Collection<T>.runningReduce(operation: (acc: S, T) -> S): List<S> {
    if (isEmpty()) return emptyList()
    val iterator = this.iterator()
    var accumulator: S = iterator.next()
    val result = ArrayList<S>(size).apply { add(accumulator) }
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
        result.add(accumulator)
    }
    return result
}


/**
 * Returns a list containing successive accumulation values generated by applying [operation] from left to right
 * to each element, its index in the original collection and current accumulator value that starts with the first element of this collection.
 *
 * Note that `acc` value passed to [operation] function should not be mutated;
 * otherwise it would affect the previous value in resulting list.
 *
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 *
 * @sample samples.collections.Collections.Aggregates.runningReduce
 */
@SinceKotlin("1.9")
public fun <S, T : S> Collection<T>.runningReduceIndexed(operation: (index: Int, acc: S, T) -> S): List<S> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return emptyList()
    var accumulator: S = iterator.next()
    val result = ArrayList<S>(size).apply { add(accumulator) }
    var index = 1
    while (iterator.hasNext()) {
        accumulator = operation(index++, accumulator, iterator.next())
        result.add(accumulator)
    }
    return result
}


/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] collection.
 */
@SinceKotlin("1.9")
public operator fun <T> Iterable<T>.plus(elements: Collection<T>): List<T> {
    if (this is Collection) return this.plus(elements)
    val result = ArrayList<T>()
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] collection.
 */
@SinceKotlin("1.9")
public operator fun <T> Collection<T>.plus(elements: Collection<T>): List<T> {
    val result = ArrayList<T>(this.size + elements.size)
    result.addAll(this)
    result.addAll(elements)
    return result
}


/**
 * Returns a list of values built from the elements of `this` collection and the [other] array with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
public inline fun <T, R, V> Collection<T>.zip(other: Array<out R>, transform: (a: T, b: R) -> V): List<V> {
    val arraySize = other.size
    val list = ArrayList<V>(minOf(size, arraySize))
    var i = 0
    for (element in this) {
        if (i >= arraySize) break
        list.add(transform(element, other[i++]))
    }
    return list
}

/**
 * Returns a list of pairs built from the elements of `this` collection and [other] collection with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
public infix fun <T, R> Iterable<T>.zip(other: Collection<R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from the elements of `this` collection and [other] collection with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
public infix fun <T, R> Collection<T>.zip(other: Collection<R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from the elements of `this` collection and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
public inline fun <T, R, V> Iterable<T>.zip(other: Collection<R>, transform: (a: T, b: R) -> V): List<V> {
    val first = iterator()
    val second = other.iterator()
    val list = ArrayList<V>(minOf(collectionSizeOrDefault1(10), other.size))
    while (first.hasNext() && second.hasNext()) {
        list.add(transform(first.next(), second.next()))
    }
    return list
}

/**
 * Returns a list of values built from the elements of `this` collection and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
public inline fun <T, R, V> Collection<T>.zip(other: Collection<R>, transform: (a: T, b: R) -> V): List<V> {
    val first = iterator()
    val second = other.iterator()
    val list = ArrayList<V>(minOf(size, other.size))
    while (first.hasNext() && second.hasNext()) {
        list.add(transform(first.next(), second.next()))
    }
    return list
}


/**
 * Returns a list of pairs built from the elements of `this` collection and [other] collection with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
public infix fun <T, R> Collection<T>.zip(other: Iterable<R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}


/**
 * Returns a list of values built from the elements of `this` collection and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
public inline fun <T, R, V> Collection<T>.zip(other: Iterable<R>, transform: (a: T, b: R) -> V): List<V> {
    val first = iterator()
    val second = other.iterator()
    val list = ArrayList<V>(minOf(size, other.collectionSizeOrDefault1(10)))
    while (first.hasNext() && second.hasNext()) {
        list.add(transform(first.next(), second.next()))
    }
    return list
}

/**
 * Returns a set containing all elements of the original set and the given [elements] collection,
 * which aren't already in this set.
 * The returned set preserves the element iteration order of the original set.
 */
@SinceKotlin("1.9")
public operator fun <T> Set<T>.plus(elements: Collection<T>): Set<T> {
    val result = LinkedHashSet<T>(mapCapacity1(elements.size))
    result.addAll(this)
    result.addAll(elements)
    return result
}


/**
 * Returns a char sequence containing characters of the original char sequence at specified [indices].
 */
@SinceKotlin("1.9")
public fun CharSequence.slice(indices: Collection<Int>): CharSequence {
    val size = indices.size
    if (size == 0) return ""
    val result = StringBuilder(size)
    for (i in indices) {
        result.append(get(i))
    }
    return result
}

/**
 * Returns a string containing characters of the original string at specified [indices].
 */
@SinceKotlin("1.9")
public fun String.slice(indices: Collection<Int>): String = (this as CharSequence).slice(indices).toString()


/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public fun UIntArray.slice(indices: Collection<Int>): List<UInt> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<UInt>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public fun ULongArray.slice(indices: Collection<Int>): List<ULong> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<ULong>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public fun UByteArray.slice(indices: Collection<Int>): List<UByte> {
    val size = indices.size
    if (size == 0) return emptyList()
    val list = ArrayList<UByte>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public fun UShortArray.slice(indices: Collection<Int>): List<UShort> {
    if (indices.isEmpty()) return emptyList()
    val list = ArrayList<UShort>(indices.size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}


/**
 * Returns a list of pairs built from the elements of `this` collection and [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public infix fun <R> UIntArray.zip(other: Collection<R>): List<Pair<UInt, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from the elements of `this` collection and [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public infix fun <R> ULongArray.zip(other: Collection<R>): List<Pair<ULong, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from the elements of `this` collection and [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public infix fun <R> UByteArray.zip(other: Collection<R>): List<Pair<UByte, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from the elements of `this` collection and [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public infix fun <R> UShortArray.zip(other: Collection<R>): List<Pair<UShort, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from the elements of `this` array and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes

public inline fun <R, V> UIntArray.zip(other: Collection<R>, transform: (a: UInt, b: R) -> V): List<V> {
    val list = ArrayList<V>(minOf(other.size, size))
    var i = 0
    for (element in other) {
        if (i >= size) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from the elements of `this` array and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes

public inline fun <R, V> ULongArray.zip(other: Collection<R>, transform: (a: ULong, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.size, arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from the elements of `this` array and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes

public inline fun <R, V> UByteArray.zip(other: Collection<R>, transform: (a: UByte, b: R) -> V): List<V> {
    val list = ArrayList<V>(minOf(other.size, size))
    var i = 0
    for (element in other) {
        if (i >= size) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from the elements of `this` array and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
public inline fun <R, V> UShortArray.zip(other: Collection<R>, transform: (a: UShort, b: R) -> V): List<V> {
    val list = ArrayList<V>(minOf(other.size, size))
    var i = 0
    for (element in other) {
        if (i >= size) break
        list.add(transform(this[i++], element))
    }
    return list
}

