// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var previous: Any? = null
    for (i in 0 .. 2) {
        class Outer {
            inner class Inner {
                override fun toString() = i.toString()
            }

            override fun toString() = Inner().toString()
        }
        if (previous != null) sb.appendLine(previous.toString())
        previous = Outer()
    }

    assertEquals("""
        0
        1

    """.trimIndent(), sb.toString())
    return "OK"
}
