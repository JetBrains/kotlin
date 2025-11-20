// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

val lazyValue: String by lazy {
    sb.appendLine("computed!")
    "Hello"
}

fun box(): String {
    sb.appendLine(lazyValue)
    sb.appendLine(lazyValue)

    assertEquals("""
        computed!
        Hello
        Hello

    """.trimIndent(), sb.toString())
    return "OK"
}
