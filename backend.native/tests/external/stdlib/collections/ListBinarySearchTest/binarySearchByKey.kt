import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.comparisons.*

val values = listOf(1, 3, 7, 10, 12, 15, 22, 45)

fun notFound(index: Int) = -(index + 1)

val comparator = compareBy<IncomparableDataItem<Int>?> { it?.value }

data class IncomparableDataItem<T>(public val value: T)

fun IncomparableDataItem<Int>.pred(): IncomparableDataItem<Int> = IncomparableDataItem(value - 1)
fun IncomparableDataItem<Int>.succ(): IncomparableDataItem<Int> = IncomparableDataItem(value + 1)
fun Int.pred() = dec()
fun Int.succ() = inc()

fun box() {
    val list = values.map { IncomparableDataItem(it) }

    list.forEachIndexed { index, item ->
        assertEquals(index, list.binarySearchBy(item.value) { it.value })
        assertEquals(notFound(index), list.binarySearchBy(item.value.pred()) { it.value })
        assertEquals(notFound(index + 1), list.binarySearchBy(item.value.succ()) { it.value })

        if (index > 0) {
            index.let { from -> assertEquals(notFound(from), list.binarySearchBy(list.first().value, fromIndex = from) { it.value }) }
            (list.size - index).let { to -> assertEquals(notFound(to), list.binarySearchBy(list.last().value, toIndex = to) { it.value }) }
        }
    }
}
