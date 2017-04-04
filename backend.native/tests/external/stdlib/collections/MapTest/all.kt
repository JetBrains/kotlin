import kotlin.test.*


fun box() {
    val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
    assertTrue(map.all { it.key != "d" })
    assertTrue(emptyMap<String, Int>().all { it.key == "d" })

    assertTrue(map.all { it.value > 0 })
    assertFalse(map.all { it.value == 2 })
}
