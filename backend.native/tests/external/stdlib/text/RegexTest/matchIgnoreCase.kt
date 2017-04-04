import kotlin.text.*

import kotlin.test.*


fun box() {
    for (input in listOf("ascii", "shrödinger"))
        assertTrue(input.toUpperCase().matches(input.toLowerCase().toRegex(RegexOption.IGNORE_CASE)))
}
