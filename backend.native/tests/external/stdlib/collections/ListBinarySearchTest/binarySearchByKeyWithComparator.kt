import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.comparisons.*

fun notFound(index: Int) = -(index + 1)

val comparator = compareBy<IncomparableDataItem<Int>?> { it?.value }

data class IncomparableDataItem<T>(public val value: T)

fun IncomparableDataItem<Int>.pred(): IncomparableDataItem<Int> = IncomparableDataItem(value - 1)
fun IncomparableDataItem<Int>.succ(): IncomparableDataItem<Int> = IncomparableDataItem(value + 1)
fun Int.pred() = dec()
fun Int.succ() = inc()

val values = listOf(1, 3, 7, 10, 12, 15, 22, 45)
fun box() {
    val list = values.map { IncomparableDataItem(IncomparableDataItem(it)) }

    list.forEachIndexed { index, item ->
        assertEquals(index, list.binarySearch { comparator.compare(it.value, item.value) })
        assertEquals(notFound(index), list.binarySearch { comparator.compare(it.value, item.value.pred()) })
        assertEquals(notFound(index + 1), list.binarySearch { comparator.compare(it.value, item.value.succ()) })

        if (index > 0) {
            index.let { from ->
                assertEquals(notFound(from), list.binarySearch(fromIndex = from) { comparator.compare(it.value, list.first().value) })
            }
            (list.size - index).let { to ->
                assertEquals(notFound(to), list.binarySearch(toIndex = to) { comparator.compare(it.value, list.last().value) })
            }
        }
    }
}
