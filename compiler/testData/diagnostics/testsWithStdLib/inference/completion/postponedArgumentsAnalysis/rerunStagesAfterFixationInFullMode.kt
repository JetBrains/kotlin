// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class ArraySortedChecker<A, T>(val array: A, val comparator: Comparator<in T>) {
    fun <R> checkSorted(sorted: A.() -> R, sortedDescending: A.() -> R, iterator: R.() -> Iterator<T>) {}
}

fun <A, T: Comparable<T>> arrayData(vararg values: T, toArray: Array<out T>.() -> A) = ArraySortedChecker<A, T>(values.toArray(), naturalOrder())

fun main() {
    with (arrayData("ac", "aD", "aba") { toList().toTypedArray() }) {}
}
