import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val comparator = compareBy<String> { it.toUpperCase().reversed() }
    val data = listOf("cat", "dad", "BAD")

    expect(listOf("BAD", "dad", "cat")) { data.sortedWith(comparator) }
    expect(listOf("cat", "dad", "BAD")) { data.sortedWith(comparator.reversed()) }
    expect(listOf("BAD", "dad", "cat")) { data.sortedWith(comparator.reversed().reversed()) }
}
