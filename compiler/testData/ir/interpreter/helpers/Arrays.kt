@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")

package kotlin.collections

public operator fun <T> Array<out T>.contains(element: T): Boolean {
    return indexOf(element) >= 0
}
public operator fun ByteArray.contains(element: Byte): Boolean {
    return indexOf(element) >= 0
}
public operator fun ShortArray.contains(element: Short): Boolean {
    return indexOf(element) >= 0
}
public operator fun IntArray.contains(element: Int): Boolean {
    return indexOf(element) >= 0
}
public operator fun LongArray.contains(element: Long): Boolean {
    return indexOf(element) >= 0
}
public operator fun BooleanArray.contains(element: Boolean): Boolean {
    return indexOf(element) >= 0
}
public operator fun CharArray.contains(element: Char): Boolean {
    return indexOf(element) >= 0
}

public fun <T> Array<out T>.indexOf(element: T): Int {
    if (element == null) {
        for (index in indices) {
            if (this[index] == null) {
                return index
            }
        }
    } else {
        for (index in indices) {
            if (element == this[index]) {
                return index
            }
        }
    }
    return -1
}
public fun ByteArray.indexOf(element: Byte): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}
public fun ShortArray.indexOf(element: Short): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}
public fun IntArray.indexOf(element: Int): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}
public fun LongArray.indexOf(element: Long): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}
public fun BooleanArray.indexOf(element: Boolean): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}
public fun CharArray.indexOf(element: Char): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

public actual fun <T> Array<out T>.asList(): List<T> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun ByteArray.asList(): List<Byte> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun ShortArray.asList(): List<Short> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun IntArray.asList(): List<Int> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun LongArray.asList(): List<Long> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun FloatArray.asList(): List<Float> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun DoubleArray.asList(): List<Double> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun BooleanArray.asList(): List<Boolean> = kotlin.UnsupportedOperationException("This is intrinsic")
public actual fun CharArray.asList(): List<Char> = kotlin.UnsupportedOperationException("This is intrinsic")

public val <T> Array<out T>.indices: IntRange
    get() = IntRange(0, lastIndex)
public val ByteArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val ShortArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val IntArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val LongArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val FloatArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val DoubleArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val BooleanArray.indices: IntRange
    get() = IntRange(0, lastIndex)
public val CharArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public inline fun <T> Array<out T>.isEmpty(): Boolean {
    return size == 0
}

public val <T> Array<out T>.lastIndex: Int
    get() = size - 1
public val ByteArray.lastIndex: Int
    get() = size - 1
public val ShortArray.lastIndex: Int
    get() = size - 1
public val IntArray.lastIndex: Int
    get() = size - 1
public val LongArray.lastIndex: Int
    get() = size - 1
public val FloatArray.lastIndex: Int
    get() = size - 1
public val DoubleArray.lastIndex: Int
    get() = size - 1
public val BooleanArray.lastIndex: Int
    get() = size - 1
public val CharArray.lastIndex: Int
    get() = size - 1

public fun ByteArray.first(): Int {
    if (isEmpty()) throw NoSuchElementException("Array is empty.")
    return this[0]
}
public fun ShortArray.first(): Int {
    if (isEmpty()) throw NoSuchElementException("Array is empty.")
    return this[0]
}
public fun IntArray.first(): Int {
    if (isEmpty()) throw NoSuchElementException("Array is empty.")
    return this[0]
}
public fun LongArray.first(): Int {
    if (isEmpty()) throw NoSuchElementException("Array is empty.")
    return this[0]
}

public inline fun ByteArray.isEmpty(): Boolean = size == 0
public inline fun ShortArray.isEmpty(): Boolean = size == 0
public inline fun IntArray.isEmpty(): Boolean = size == 0
public inline fun LongArray.isEmpty(): Boolean = size == 0

public fun <T> Array<out T>.toList(): List<T> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}
public fun IntArray.toList(): List<Int> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

public fun <T> Array<out T>.toMutableList(): MutableList<T> = kotlin.UnsupportedOperationException("This is intrinsic")
public fun IntArray.toMutableList(): MutableList<Int> {
    val list = ArrayList<Int>(size)
    for (item in this) list.add(item)
    return list
}

public fun <T, C : MutableCollection<in T>> Array<out T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

public fun <T> Array<out T>.toSet(): Set<T> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<T>(mapCapacity(size)))
    }
}

public fun <T> Array<out T>.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

public fun <T, A : Appendable> Array<out T>.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            buffer.appendElement(element, transform)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

private fun <T> Appendable.appendElement(element: T, transform: ((T) -> CharSequence)?) {
    when {
        transform != null -> append(transform(element))
        element is CharSequence? -> append(element)
        element is Char -> append(element)
        else -> append(element.toString())
    }
}

public fun <T> Array<out T>.asSequence(): Sequence<T> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}