import kotlin.test.*
import kotlin.comparisons.*

fun <T> assertSorted(list: List<T>, cmp: Comparator<in T>, message: String = "") {
    if (list.isEmpty() || list.size == 1) {
        return
    }
    val it = list.iterator()
    var prev = it.next()
    while(it.hasNext()) {
        val cur = it.next()
        assert(cmp.compare(prev, cur) <= 0)
        prev = cur
    }
}

fun box() {
    fun String.nullIfEmpty() = if (isEmpty()) null else this
    listOf(null, "", "a").let {
        assertSorted(it.sortedWith(nullsFirst(compareBy { it })), nullsFirst(compareBy { it }))
        assertSorted(it.sortedWith(nullsLast(compareByDescending { it })), compareByDescending { it })
        assertSorted(
                it.sortedWith(nullsFirst(compareByDescending { it.nullIfEmpty() })),
                nullsFirst(compareByDescending { it.nullIfEmpty() })
        )
    }
}
