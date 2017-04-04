import kotlin.test.*


fun box() {
    val list = listOf('A', 'B', 'C', 'D')
    // ABCD
    // 0123
    assertEquals(listOf('B', 'C', 'D'), list.slice(1..3))
    assertEquals(listOf('D', 'C', 'B'), list.slice(3 downTo 1))

    val iter = listOf(2, 0, 3)
    assertEquals(listOf('C', 'A', 'D'), list.slice(iter))
}
