import kotlin.test.*

fun box() {
    assertEquals(listOf(2, 3), arrayOf("", "bc", "def").mapNotNull { if (it.isEmpty()) null else it.length })
}