import kotlin.test.*


fun box() {
    val map = listOf("a", "bb", "ccc").associateBy { it.length }
    assertEquals(3, map.size)
    assertEquals("a", map.get(1))
    assertEquals("bb", map.get(2))
    assertEquals("ccc", map.get(3))
}
