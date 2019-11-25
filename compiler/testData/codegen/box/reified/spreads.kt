// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun <T> foo(vararg a: T) = a.size

inline fun <reified T> bar(a: Array<T>, block: () -> T): Array<T> {
    assertEquals(4, foo(*a, block(), block()))

    return arrayOf(*a, block(), block())
}

inline fun <reified T> empty() = arrayOf<T>()

fun box(): String {

    var i = 0
    val a: Array<String> = bar(arrayOf("1", "2")) { i++; i.toString() }
    assertEquals("1234", a.joinToString(""))

    i = 0
    val b: Array<Int> = bar(arrayOf(0, 1)) { i++ }
    assertEquals("0123", b.map { it.toString() }.joinToString(""))

    return "OK"
}
