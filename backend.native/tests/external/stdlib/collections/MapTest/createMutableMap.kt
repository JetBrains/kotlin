import kotlin.test.*


fun box() {
    val map = mutableMapOf("b" to 1, "c" to 2)
    map.put("a", 3)
    assertEquals(listOf("b" to 1, "c" to 2, "a" to 3), map.toList())
}
