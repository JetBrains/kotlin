import kotlin.test.*


fun box() {
    val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
    assertTrue(map.any())
    assertFalse(emptyMap<String, Int>().any())

    assertTrue(map.any { it.key == "b" })
    assertFalse(emptyMap<String, Int>().any { it.key == "b" })

    assertTrue(map.any { it.value == 2 })
    assertFalse(map.any { it.value == 5 })
}
