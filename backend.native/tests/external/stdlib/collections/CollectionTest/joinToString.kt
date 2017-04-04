import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    val text = data.joinToString("-", "<", ">")
    assertEquals("<foo-bar>", text)

    val mixed = listOf('a', "b", StringBuilder("c"), null, "d", 'e', 'f')
    val text2 = mixed.joinToString(limit = 4, truncated = "*")
    assertEquals("a, b, c, null, *", text2)
}
