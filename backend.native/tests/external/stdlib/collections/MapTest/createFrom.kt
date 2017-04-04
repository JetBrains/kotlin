import kotlin.test.*


fun box() {
    val pairs = arrayOf("a" to 1, "b" to 2)
    val expected = mapOf(*pairs)

    assertEquals(expected, pairs.toMap())
    assertEquals(expected, pairs.asIterable().toMap())
    assertEquals(expected, pairs.asSequence().toMap())
    assertEquals(expected, expected.toMap())
    assertEquals(mapOf("a" to 1), expected.filterKeys { it == "a" }.toMap())
    assertEquals(emptyMap(), expected.filter { false }.toMap())

    val mutableMap = expected.toMutableMap()
    assertEquals(expected, mutableMap)
    mutableMap += "c" to 3
    assertNotEquals(expected, mutableMap)
}
