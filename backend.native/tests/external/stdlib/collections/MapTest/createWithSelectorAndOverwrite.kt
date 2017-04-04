import kotlin.test.*


fun box() {
    val map = listOf("aa", "bb", "ccc").associateBy { it.length }
    assertEquals(2, map.size)
    assertEquals("bb", map.get(2))
    assertEquals("ccc", map.get(3))
}
