import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    fun String.nullIfEmpty() = if (isEmpty()) null else this
    listOf(null, "", "a").let {
        expect(listOf(null, "", "a")) { it.sortedWith(nullsFirst(compareBy { it })) }
        expect(listOf("a", "", null)) { it.sortedWith(nullsLast(compareByDescending { it })) }
        expect(listOf(null, "a", "")) { it.sortedWith(nullsFirst(compareByDescending { it.nullIfEmpty() })) }
    }
}
