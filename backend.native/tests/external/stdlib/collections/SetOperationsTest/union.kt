import kotlin.test.*


fun box() {
    assertEquals(listOf(1, 3, 5), listOf(1, 3).union(listOf(5)).toList())
    assertEquals(listOf(1), listOf<Int>().union(listOf(1)).toList())
}
