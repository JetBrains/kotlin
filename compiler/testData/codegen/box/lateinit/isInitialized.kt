// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class A {
    lateinit var s: String

    fun foo() {
        sb.appendLine(::s.isInitialized)
    }
}

fun box(): String {
    val a = A()
    a.foo()
    a.s = "zzz"
    a.foo()

    assertEquals("""
        false
        true

    """.trimIndent(), sb.toString())
    return "OK"
}
