import kotlin.test.*


fun box() {
    assertTrue(listOf(1, 3).intersect(listOf(5)).none())
    assertEquals(listOf(5), listOf(1, 3, 5).intersect(listOf(5)).toList())
    assertEquals(listOf(1, 3, 5), listOf(1, 3, 5).intersect(listOf(1, 3, 5)).toList())
    assertTrue(listOf<Int>().intersect(listOf(1)).none())
}
