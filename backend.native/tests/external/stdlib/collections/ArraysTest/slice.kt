import kotlin.test.*

fun box() {
    val iter: Iterable<Int> = listOf(3, 1, 2)

    assertEquals(listOf("B"), arrayOf("A", "B", "C").slice(1..1))
    assertEquals(listOf('E', 'B', 'C'), arrayOf('A', 'B', 'C', 'E').slice(iter))

    assertEquals(listOf<Int>(), arrayOf<Int>().slice(5..4))
    assertEquals(listOf<Int>(), intArrayOf(1, 2, 3).slice(5..1))
    assertEquals(listOf(2, 3, 9), intArrayOf(2, 3, 9, 2, 3, 9).slice(iter))
    assertEquals(listOf(2.0, 3.0), doubleArrayOf(2.0, 3.0, 9.0).slice(0..1))
    assertEquals(listOf(2f, 3f), floatArrayOf(2f, 3f, 9f).slice(0..1))
    assertEquals(listOf<Byte>(127, 100), byteArrayOf(50, 100, 127).slice(2 downTo 1))
    assertEquals(listOf<Short>(200, 100), shortArrayOf(50, 100, 200).slice(2 downTo 1))
    assertEquals(listOf(100L, 200L, 30L), longArrayOf(50L, 100L, 200L, 30L).slice(1..3))
    assertEquals(listOf(true, false, true), booleanArrayOf(true, false, true, true).slice(iter))
}
