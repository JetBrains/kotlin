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
    val list = values.flatMap { v1 -> values.map { v2 -> Pair(v1, v2) } }

    list.forEachIndexed { index, item ->
        assertEquals(index, list.binarySearch { compareValuesBy(it, item, { it.first }, { it.second }) })
    }
}
