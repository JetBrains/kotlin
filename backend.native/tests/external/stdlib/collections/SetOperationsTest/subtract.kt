import kotlin.test.*


fun box() {
    assertEquals(listOf(1, 3), listOf(1, 3).subtract(listOf(5)).toList())
    assertEquals(listOf(1, 3), listOf(1, 3, 5).subtract(listOf(5)).toList())
    assertTrue(listOf(1, 3, 5).subtract(listOf(1, 3, 5)).none())
    assertTrue(listOf<Int>().subtract(listOf(1)).none())
}
