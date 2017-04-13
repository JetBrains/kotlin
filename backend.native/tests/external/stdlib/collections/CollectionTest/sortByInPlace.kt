import kotlin.test.*

fun <T: Comparable<T>> assertSorted(list: List<out T>, message: String = "") = assertSorted(list, message, { it })

inline fun <T, R : Comparable<R>> assertSorted(list: List<out T>, message: String = "", crossinline selector: (T) -> R) {
    if (list.isEmpty() || list.size == 1) {
        return
    }
    val it = list.iterator()
    var prev = selector(it.next())
    while(it.hasNext()) {
        val cur = selector(it.next())
        assertTrue(prev.compareTo(cur) <= 0, message)
        prev = cur
    }
}

inline fun <T, R : Comparable<R>> assertSortedDescending(list: List<out T>, message: String = "", crossinline selector: (T) -> R) {
    if (list.isEmpty() || list.size == 1) {
        return
    }
    val it = list.iterator()
    var prev = selector(it.next())
    while(it.hasNext()) {
        val cur = selector(it.next())
        assertTrue(prev.compareTo(cur) >= 0, message)
        prev = cur
    }
}

fun box() {
    val data = arrayListOf("aa" to 20, "ab" to 3, "aa" to 3)
    data.sortBy { it.second }
    assertSorted(data) { it.second }

    data.sortBy { it.first }
    assertSorted(data) { it.first }

    data.sortByDescending { (it.first + it.second).length }
    assertSortedDescending(data) { (it.first + it.second).length }
}
