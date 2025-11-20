// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun foo(vararg args: Any?) {
    for (arg in args) {
        sb.appendLine(arg.toString())
    }
}

fun bar(vararg args: Any?) {
    foo(1, *args, 2, *args, 3)
}

fun box(): String {
    bar(null, true, "Hello")

    assertEquals("""
        1
        null
        true
        Hello
        2
        null
        true
        Hello
        3

    """.trimIndent(), sb.toString())
    return "OK"
}
