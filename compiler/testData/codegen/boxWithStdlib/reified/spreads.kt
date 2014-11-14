import kotlin.test.assertEquals

fun <T> foo(vararg a: T) = a.size()

inline fun <reified T> bar(a: Array<T>, block: () -> T): Array<T> {
    assertEquals(4, foo(*a, block(), block()))

    return array(*a, block(), block())
}

inline fun <reified T> empty() = array<T>()

fun box(): String {

    var i = 0
    val a: Array<String> = bar(array("1", "2")) { i++; i.toString() }
    assertEquals("1234", a.join(""))

    i = 0
    val b: Array<Int> = bar(array(0, 1)) { i++ }
    assertEquals("0123", b.map { it.toString() }.join(""))

    return "OK"
}
