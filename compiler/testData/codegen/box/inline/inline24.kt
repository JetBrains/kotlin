// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()
fun myPrintln(s: String): Unit {
    sb.appendLine(s)
}

fun foo() = myPrintln("foo")
fun bar() = myPrintln("bar")

inline fun baz(x: Unit = foo(), y: Unit) {}

fun box(): String {
    baz(y = bar())

    assertEquals("""
        bar
        foo

    """.trimIndent(), sb.toString())
    return "OK"
}
