// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        sb.appendLine("Before")
        foo()
        sb.appendLine("After")
    } catch (e: Throwable) {
        sb.appendLine("Caught Throwable")
    }

    sb.appendLine("Done")

    assertEquals("""
        Before
        Caught Throwable
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo() {
    throw Error("Error happens")
    sb.appendLine("After in foo()")
}
