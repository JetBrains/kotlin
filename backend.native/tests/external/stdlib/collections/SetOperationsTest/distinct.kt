import kotlin.test.*


fun box() {
    assertEquals(listOf(1, 3, 5), listOf(1, 3, 3, 1, 5, 1, 3).distinct())
    assertTrue(listOf<Int>().distinct().isEmpty())
}
