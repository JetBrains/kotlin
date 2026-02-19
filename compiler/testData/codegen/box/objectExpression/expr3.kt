// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var cnt = 0

    var x: Any = ""

    for (i in 0 .. 1) {
        sb.appendLine(x)
        cnt++
        val y = object {
            override fun toString() = cnt.toString()
        }
        x = y
    }
    sb.appendLine(x)

    assertEquals("""
        
        1
        2

    """.trimIndent(), sb.toString())
    return "OK"
}
