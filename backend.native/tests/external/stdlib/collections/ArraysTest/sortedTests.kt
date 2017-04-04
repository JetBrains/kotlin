import kotlin.test.*

class ArraySortedChecker<A, T>(val array: A, val comparator: Comparator<in T>) {
    public fun <R> checkSorted(sorted: A.() -> R, sortedDescending: A.() -> R, iterator: R.() -> Iterator<T>) {
        array.sorted().iterator().assertSorted { a, b -> comparator.compare(a, b) <= 0 }
        array.sortedDescending().iterator().assertSorted { a, b -> comparator.compare(a, b) >= 0 }
    }
}

fun box() {
    assertTrue(arrayOf<Long>().sorted().none())
    assertEquals(listOf(1), arrayOf(1).sorted())

    fun <A, T : Comparable<T>> arrayData(vararg values: T, toArray: Array<out T>.() -> A) = ArraySortedChecker<A, T>(values.toArray(), naturalOrder())

    with(arrayData("ac", "aD", "aba") { toList().toTypedArray() }) {
        checkSorted<List<String>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<Array<String>>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }

    with(arrayData("ac", "aD", "aba") { toList().toTypedArray() as Array<out String> }) {
        checkSorted<List<String>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<Array<out String>>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }

    with(arrayData(3, 7, 1) { toIntArray() }) {
        checkSorted<List<Int>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<IntArray>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }


    with(arrayData(1L, Long.MIN_VALUE, Long.MAX_VALUE) { toLongArray() }) {
        checkSorted<List<Long>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<LongArray>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }

    with(arrayData('a', 'D', 'c') { toCharArray() }) {
        checkSorted<List<Char>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<CharArray>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }

    with(arrayData(1.toByte(), Byte.MAX_VALUE, Byte.MIN_VALUE) { toByteArray() }) {
        checkSorted<List<Byte>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<ByteArray>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }

    with(arrayData(Double.POSITIVE_INFINITY, 1.0, Double.MAX_VALUE) { toDoubleArray() }) {
        checkSorted<List<Double>>({ sorted() }, { sortedDescending() }, { iterator() })
        checkSorted<DoubleArray>({ sortedArray() }, { sortedArrayDescending() }, { iterator() })
    }
}
