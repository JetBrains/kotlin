// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

lateinit var s: String

fun foo() {
    sb.appendLine(::s.isInitialized)
}

fun box(): String {
    foo()
    s = "zzz"
    foo()

    assertEquals("""
        false
        true

    """.trimIndent(), sb.toString())
    return "OK"
}
