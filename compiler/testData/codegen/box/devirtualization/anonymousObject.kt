// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

interface I {
    fun foo()
}

fun test() {
    val impl = object : I {
        override fun foo() { sb.append("zzz") }
    }

    val delegating = object: I by impl { }

    delegating.foo()
}

fun box(): String {
    test()
    assertEquals("zzz", sb.toString())

    return "OK"
}
