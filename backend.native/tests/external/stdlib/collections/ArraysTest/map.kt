import kotlin.test.*

fun box() {
    assertEquals(listOf(1, 2, 4), arrayOf("a", "bc", "test").map { it.length })
    assertEquals(listOf('a', 'b', 'c'), intArrayOf(1, 2, 3).map { 'a' + it - 1 })
    assertEquals(listOf(1, 2, 3), longArrayOf(1000, 2000, 3000).map { (it / 1000).toInt() })
    assertEquals(listOf(1.0, 0.5, 0.4, 0.2, 0.1), doubleArrayOf(1.0, 2.0, 2.5, 5.0, 10.0).map { 1 / it })
}
