import kotlin.text.*

import kotlin.test.*


fun box() {
    val input = "test"
    val pattern = ".*".toRegex()
    val matches = pattern.findAll(input).toList()
    assertEquals(input, matches[0].value)
    assertEquals(input, matches.joinToString("") { it.value })
    assertEquals(2, matches.size)
}
