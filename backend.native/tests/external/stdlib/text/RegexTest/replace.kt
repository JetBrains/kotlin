import kotlin.text.*

import kotlin.test.*


fun box() {
    val input = "123-456"
    val pattern = "(\\d+)".toRegex()
    assertEquals("(123)-(456)", pattern.replace(input, "($1)"))

    assertEquals("$&-$&", pattern.replace(input, Regex.escapeReplacement("$&")))
    assertEquals("X-456", pattern.replaceFirst(input, "X"))
}
