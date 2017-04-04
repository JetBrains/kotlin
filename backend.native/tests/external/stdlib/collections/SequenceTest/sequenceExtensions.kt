import kotlin.test.*

import kotlin.comparisons.*

fun <T, C : MutableCollection<in T>> Sequence<T>.takeWhileTo(result: C, predicate: (T) -> Boolean): C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

fun box() {
    val d = ArrayList<Int>()
    sequenceOf(0, 1, 2, 3, 4, 5).takeWhileTo(d, { i -> i < 4 })
    assertEquals(4, d.size)
}
