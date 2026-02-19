// WITH_STDLIB

// FILE: lib.kt
val sb = StringBuilder()
fun myPrintln(s: String): Unit {
    sb.appendLine(s)
}

fun foo() = myPrintln("foo")

inline fun baz(x: Unit = foo(), y: Unit) {}

// FILE: main.kt
import kotlin.test.*

fun bar() = myPrintln("bar")

fun box(): String {
    baz(y = bar())

    assertEquals("""
        bar
        foo

    """.trimIndent(), sb.toString())
    return "OK"
}
