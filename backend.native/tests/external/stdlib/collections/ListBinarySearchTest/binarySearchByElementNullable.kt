import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.comparisons.*

val values = listOf(1, 3, 7, 10, 12, 15, 22, 45)

fun notFound(index: Int) = -(index + 1)

fun box() {
    val list = listOf(null) + values
    list.forEachIndexed { index, item ->
        assertEquals(index, list.binarySearch(item))

        if (index > 0) {
            index.let { from -> assertEquals(notFound(from), list.binarySearch(list.first(), fromIndex = from)) }
            (list.size - index).let { to -> assertEquals(notFound(to), list.binarySearch(list.last(), toIndex = to)) }
        }
    }
}
