// WITH_STDLIB

// FILE: lib.kt
inline fun <reified T> copy(c: Collection<T>): Array<T> {
    return c.toTypedArray()
}

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val a: Array<String> = copy(listOf("a", "b", "c"))
    assertEquals("abc", a.joinToString(""))

    val b: Array<Int> = copy(listOf(1,2,3))
    assertEquals("123", b.map { it.toString() }.joinToString(""))

    return "OK"
}
