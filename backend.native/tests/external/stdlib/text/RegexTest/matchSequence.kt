import kotlin.text.*

import kotlin.test.*


fun box() {
    val input = "123 456 789"
    val pattern = "\\d+".toRegex()

    val matches = pattern.findAll(input)
    val values = matches.map { it.value }
    val expected = listOf("123", "456", "789")
    assertEquals(expected, values.toList())
    assertEquals(expected, values.toList(), "running match sequence second time")
    assertEquals(expected.drop(1), pattern.findAll(input, startIndex = 3).map { it.value }.toList())

    assertEquals(listOf(0..2, 4..6, 8..10), matches.map { it.range }.toList())
}
