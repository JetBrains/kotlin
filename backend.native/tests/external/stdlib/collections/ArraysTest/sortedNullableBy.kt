import kotlin.test.*

fun box() {
    fun String.nullIfEmpty() = if (this.isEmpty()) null else this
    arrayOf(null, "").let {
        expect(listOf(null, "")) { it.sortedWith(nullsFirst(compareBy { it })) }
        expect(listOf("", null)) { it.sortedWith(nullsLast(compareByDescending { it })) }
        expect(listOf("", null)) { it.sortedWith(nullsLast(compareByDescending { it.nullIfEmpty() })) }
    }
}
