import kotlin.test.*


fun box() {
    val map = listOf("a", "bb", "ccc").associateBy({ it.length }, { it.toUpperCase() })
    assertEquals(3, map.size)
    assertEquals("A", map[1])
    assertEquals("BB", map[2])
    assertEquals("CCC", map[3])
}
