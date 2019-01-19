// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun <T> foo(vararg a: T) = a.size

inline fun <reified T> bar(block: () -> T): Array<T> {
    assertEquals(2, foo(block(), block()))

    return arrayOf(block(), block(), block())
}

inline fun <reified T> empty() = arrayOf<T>()

fun box(): String {
    var i = 0
    val a: Array<String> = bar() { i++; i.toString() }
    assertEquals("345", a.joinToString(""))

    i = 0
    val b: Array<Int> = bar() { i++ }
    assertEquals("234", b.map { it.toString() }.joinToString(""))

    val c: Array<String> = empty()
    assertEquals(0, c.size)

    return "OK"
}
