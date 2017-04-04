import kotlin.test.*

private class ArraySortedChecker<A, T>(val array: A, val comparator: Comparator<in T>) {
    public fun <R> checkSorted(sorted: A.() -> R, sortedDescending: A.() -> R, iterator: R.() -> Iterator<T>) {
        array.sorted().iterator().assertSorted { a, b -> comparator.compare(a, b) <= 0 }
        array.sortedDescending().iterator().assertSorted { a, b -> comparator.compare(a, b) >= 0 }
    }
}

fun box() {
    val comparator = compareBy { it: Int -> it % 3 }.thenByDescending { it }
    fun <A, T> arrayData(array: A, comparator: Comparator<T>) = ArraySortedChecker<A, T>(array, comparator)

    arrayData(intArrayOf(0, 1, 2, 3, 4, 5), comparator)
            .checkSorted<List<Int>>({ sortedWith(comparator) }, { sortedWith(comparator.reversed()) }, { iterator() })

    arrayData(arrayOf(0, 1, 2, 3, 4, 5), comparator)
            .checkSorted<Array<out Int>>({ sortedArrayWith(comparator) }, { sortedArrayWith(comparator.reversed()) }, { iterator() })

    // in-place
    val array = Array(6) { it }
    array.sortWith(comparator)
    array.iterator().assertSorted { a, b -> comparator.compare(a, b) <= 0 }
}
