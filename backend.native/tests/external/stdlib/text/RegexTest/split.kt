import kotlin.text.*

import kotlin.test.*


fun box() {
    val input = """
     some  ${"\t"}  word
     split
    """.trim()

    assertEquals(listOf("some", "word", "split"), "\\s+".toRegex().split(input))

    assertEquals(listOf("name", "value=5"), "=".toRegex().split("name=value=5", limit = 2))

}
