// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val str = "captured"
    foo {
        sb.appendLine(it)
        sb.appendLine(str)
    }
    assertEquals("""
        42
        captured

    """.trimIndent(), sb.toString())
    return "OK"

}

fun foo(f: (Int) -> Unit) {
    f(42)
}
