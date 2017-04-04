import kotlin.test.*

val STRING_CASE_INSENSITIVE_ORDER: Comparator<String> = compareBy { it: String -> it.toUpperCase() }.thenBy { it.toLowerCase() }.thenBy { it }
fun box() {
    assertEquals(null, arrayOf<Int>().minWith(naturalOrder()))
    assertEquals("a", arrayOf("a", "B").minWith(STRING_CASE_INSENSITIVE_ORDER))
}
