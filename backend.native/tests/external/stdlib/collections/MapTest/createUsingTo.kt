import kotlin.test.*


fun box() {
    val map = mapOf("a" to 1, "b" to 2)
    assertEquals(2, map.size)
    assertEquals(1, map["a"])
    assertEquals(2, map["b"])
}
