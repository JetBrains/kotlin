import kotlin.text.*

import kotlin.test.*


fun box() {
    val regex = "^[a-z]*$".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    val matchedValues = regex.findAll("test\n\nLine").map { it.value }.toList()
    assertEquals(listOf("test", "", "Line"), matchedValues)
}
