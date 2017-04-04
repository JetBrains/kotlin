import kotlin.test.*



fun box() {
    val iter = listOf(4, 3, 0, 1)
    // abcde
    // 01234
    assertEquals("bcd", "abcde".substring(1..3))
    assertEquals("dcb", "abcde".slice(3 downTo 1))
    assertEquals("edab", "abcde".slice(iter))
}
