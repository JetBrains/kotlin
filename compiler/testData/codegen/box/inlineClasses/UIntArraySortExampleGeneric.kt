// KJS_WITH_FULL_RUNTIME
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(private val value: T) : Comparable<UInt<T>> {
    companion object {
        private const val INT_MASK = 0xffffffffL
    }

    fun asInt(): Int = value

    fun toLong(): Long = value.toLong() and INT_MASK

    override fun compareTo(other: UInt<T>): Int =
        flip().compareTo(other.flip())

    override fun toString(): String {
        return toLong().toString()
    }

    private fun flip(): Int =
        value xor Int.MIN_VALUE
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class UIntArray(private val intArray: IntArray) {
    val size: Int get() = intArray.size

    operator fun get(index: Int): UInt<Int> = UInt(intArray[index])

    operator fun set(index: Int, value: UInt<Int>) {
        intArray[index] = value.asInt()
    }

    operator fun iterator(): UIntIterator = UIntIterator(intArray.iterator())
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class UIntIterator(private val intIterator: IntIterator) : Iterator<UInt<Int>> {
    override fun next(): UInt<Int> {
        return UInt(intIterator.next())
    }

    override fun hasNext(): Boolean {
        return intIterator.hasNext()
    }
}

fun uIntArrayOf(vararg u: Int): UIntArray = UIntArray(u)

fun UIntArray.swap(i: Int, j: Int) {
    this[j] = this[i].also { this[i] = this[j] }
}

fun UIntArray.quickSort() {
    quickSort(0, size - 1)
}

private fun UIntArray.quickSort(l: Int, r: Int) {
    if (l < r) {
        val q = partition(l, r)
        quickSort(l, q - 1)
        quickSort(q + 1, r)
    }
}

private fun UIntArray.partition(l: Int, r: Int): Int {
    val m = this[(l + r) / 2]
    var i = l
    var j = r
    while (i <= j) {
        while (this[i] < m) i++
        while (this[j] > m) j--
        if (i <= j)
            swap(i++, j--)
    }

    return i
}

fun check(array: UIntArray, resultAsInt: String, resultAsInner: String) {
    val actualAsInt = StringBuilder()
    val actualAsInner = StringBuilder()
    for (n in array) {
        actualAsInt.append("${n.asInt()} ")
        actualAsInner.append(n.toString() + " ")
    }

    if (actualAsInt.toString() != resultAsInt) {
        throw IllegalStateException("wrong result as int (actual): $actualAsInt ; expected: $resultAsInt")
    }

    if (actualAsInner.toString() != resultAsInner) {
        throw IllegalStateException("wrong result as inner (actual): $actualAsInner ; expected: $resultAsInner")
    }
}

fun box(): String {
    val a1 = uIntArrayOf(1, 2, 3)
    a1.quickSort()

    check(a1, "1 2 3 ", "1 2 3 ")

    val a2 = uIntArrayOf(-1)
    a2.quickSort()

    check(a2, "-1 ", "4294967295 ")

    val a3 = uIntArrayOf(-1, 1, 0)
    a3.quickSort()

    check(a3, "0 1 -1 ", "0 1 4294967295 ")

    val a4 = uIntArrayOf(-1, Int.MAX_VALUE)
    a4.quickSort()

    check(a4, "${Int.MAX_VALUE} -1 ", "2147483647 4294967295 ")

    return "OK"
}