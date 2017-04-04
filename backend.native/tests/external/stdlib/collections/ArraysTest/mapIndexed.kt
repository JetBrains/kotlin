import kotlin.test.*

fun box() {
    assertEquals(listOf(1, 1, 2), arrayOf("a", "bc", "test").mapIndexed { index, s -> s.length - index })
    assertEquals(listOf(0, 2, 2), intArrayOf(3, 2, 1).mapIndexed { index, i -> i * index })
    assertEquals(listOf("0;20", "1;21", "2;22"), longArrayOf(20, 21, 22).mapIndexed { index, it -> "$index;$it" })
}
