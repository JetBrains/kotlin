import kotlin.test.*

fun box() {
    val text = arrayOf("foo", "bar").joinToString("-", "<", ">")
    assertEquals("<foo-bar>", text)

    val text2 = arrayOf('a', "b", StringBuilder("c"), null, "d", 'e', 'f').joinToString(limit = 4, truncated = "*")
    assertEquals("a, b, c, null, *", text2)

    val text3 = intArrayOf(1, 2, 5, 8).joinToString("+", "[", "]")
    assertEquals("[1+2+5+8]", text3)

    val text4 = charArrayOf('f', 'o', 'o').joinToString()
    assertEquals("f, o, o", text4)
}
