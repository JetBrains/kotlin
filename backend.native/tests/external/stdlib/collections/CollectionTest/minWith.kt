import kotlin.test.*
import kotlin.comparisons.*

val STRING_CASE_INSENSITIVE_ORDER: Comparator<String> = compareBy { it: String -> it.toUpperCase() }.thenBy { it.toLowerCase() }.thenBy { it }
fun box() {
    expect(null, { listOf<Int>().minWith(naturalOrder()) })
    expect(1, { listOf(1).minWith(naturalOrder()) })
    expect("a", { listOf("a", "B").minWith(STRING_CASE_INSENSITIVE_ORDER) })
    expect("a", { listOf("a", "B").asSequence().minWith(STRING_CASE_INSENSITIVE_ORDER) })
}
