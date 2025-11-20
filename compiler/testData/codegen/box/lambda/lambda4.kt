// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val lambda = bar()
    lambda()
    lambda()

    assertEquals("""
        1
        2
        3
        3
        4

    """.trimIndent(), sb.toString())
    return "OK"
}

fun bar(): () -> Unit {
    var x = Integer(0)

    val lambda = {
        sb.appendLine(x.toString())
        x = x + 1
    }

    x = x + 1

    lambda()
    lambda()

    sb.appendLine(x.toString())

    return lambda
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}
