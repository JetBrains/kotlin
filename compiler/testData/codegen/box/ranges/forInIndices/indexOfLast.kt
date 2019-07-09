// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun <T> Array<out T>.indexOfLast(predicate: (T) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

val ints = arrayOf(1, 2, 3, 2, 1)

fun box(): String {
    assertEquals(-1, ints.indexOfLast { it == 4 })
    assertEquals(4, ints.indexOfLast { it == 1 })
    assertEquals(3, ints.indexOfLast { it == 2 })
    assertEquals(2, ints.indexOfLast { it == 3 })
    assertEquals(-1, ints.indexOfLast { it == 0 })
    return "OK"
}