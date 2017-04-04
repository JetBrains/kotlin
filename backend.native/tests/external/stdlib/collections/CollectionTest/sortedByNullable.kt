import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    fun String.nonEmptyLength() = if (isEmpty()) null else length
    listOf("", "sort", "abc").let {
        assertEquals(listOf("", "abc", "sort"), it.sortedBy { it.nonEmptyLength() })
        assertEquals(listOf("sort", "abc", ""), it.sortedByDescending { it.nonEmptyLength() })
        assertEquals(listOf("abc", "sort", ""), it.sortedWith(compareBy(nullsLast<Int>()) { it.nonEmptyLength() }))
    }
}
