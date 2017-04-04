import kotlin.test.*
import kotlin.comparisons.*

val STRING_CASE_INSENSITIVE_ORDER: Comparator<String> = compareBy { it: String -> it.toUpperCase() }.thenBy { it.toLowerCase() }.thenBy { it }
fun box() {
    expect(null, { listOf<Int>().maxWith(naturalOrder()) })
    expect(1, { listOf(1).maxWith(naturalOrder()) })
    expect("B", { listOf("a", "B").maxWith(STRING_CASE_INSENSITIVE_ORDER) })
    expect("B", { listOf("a", "B").asSequence().maxWith(STRING_CASE_INSENSITIVE_ORDER) })
}
