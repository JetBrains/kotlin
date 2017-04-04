import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val words = listOf("a", "abc", "ab", "def", "abcd")
    val byLength = words.groupBy { it.length }
    assertEquals(4, byLength.size)

    // verify that order of keys is preserved
    assertEquals(listOf(
            1 to listOf("a"),
            3 to listOf("abc", "def"),
            2 to listOf("ab"),
            4 to listOf("abcd")
    ), byLength.toList())

    val l3 = byLength[3].orEmpty()
    assertEquals(listOf("abc", "def"), l3)
}
