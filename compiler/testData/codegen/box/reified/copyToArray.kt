// WITH_STDLIB

import kotlin.test.assertEquals

inline fun <reified T> copy(c: Collection<T>): Array<T> {
    return c.toTypedArray()
}

fun box(): String {
    val a: Array<String> = copy(listOf("a", "b", "c"))
    assertEquals("abc", a.joinToString(""))

    val b: Array<Int> = copy(listOf(1,2,3))
    assertEquals("123", b.map { it.toString() }.joinToString(""))

    return "OK"
}
