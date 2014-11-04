import kotlin.test.assertEquals

inline fun <reified T> copy(c: Collection<T>): Array<T> {
    return c.copyToArray()
}

fun box(): String {
    val a: Array<String> = copy(listOf("a", "b", "c"))
    assertEquals("abc", a.join(""))

    val b: Array<Int> = copy(listOf(1,2,3))
    assertEquals("123", b.map { it.toString() }.join(""))

    return "OK"
}
