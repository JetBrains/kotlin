// WITH_STDLIB

import kotlin.test.*

// TODO: check IR
fun constantIndent(): String {
    return """
        Hello,
        World
    """.trimIndent()
}

fun constantMargin(): String {
    return """
        |Hello,
        |World
    """.trimMargin()
}

fun box(): String {
    assertTrue(constantIndent() === constantIndent())
    assertTrue(constantMargin() === constantMargin())

    return "OK"
}
