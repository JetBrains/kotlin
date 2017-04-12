import kotlin.test.*

fun <T: Comparable<T>> assertSorted(array: Array<out T>, message: String = "") = assertSorted(array, message, { it })

inline fun <T, R : Comparable<R>> assertSorted(array: Array<out T>, message: String = "", crossinline selector: (T) -> R) {
    if (array.isEmpty() || array.size == 1) {
        return
    }
    var prev = selector(array[0])
    for (i in 1..array.lastIndex) {
        val cur = selector(array[i])
        assertTrue(prev.compareTo(cur) <= 0, message)
        prev = cur
    }
}

inline fun <T, R : Comparable<R>> assertSortedDescending(array: Array<out T>, message: String = "", crossinline selector: (T) -> R) {
    if (array.isEmpty() || array.size == 1) {
        return
    }
    var prev = selector(array[0])
    for (i in 1..array.lastIndex) {
        val cur = selector(array[i])
        assertTrue(prev.compareTo(cur) >= 0, message)
        prev = cur
    }
}

fun box() {
    val data = arrayOf("aa" to 20, "ab" to 3, "aa" to 3)
    data.sortBy { it.second }
    assertSorted(data) { it.second }

    data.sortBy { it.first }
    assertSorted(data) { it.first }

    data.sortByDescending { (it.first + it.second).length }
    assertSortedDescending(data) { (it.first + it.second).length }
}
