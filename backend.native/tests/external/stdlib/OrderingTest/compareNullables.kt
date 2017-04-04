import kotlin.test.*

import kotlin.comparisons.*

data class Item(val name: String, val rating: Int) : Comparable<Item> {
    public override fun compareTo(other: Item): Int {
        return compareValuesBy(this, other, { it.rating }, { it.name })
    }
}

val v1 = Item("wine", 9)
val v2 = Item("beer", 10)

fun box() {
    val v1: Item? = v1
    val v2: Item? = null
    val diff = compareValuesBy(v1, v2) { it?.rating }
    assertTrue(diff > 0)
    val diff2 = nullsLast(compareBy<Item> { it.rating }.thenBy { it.name }).compare(v1, v2)
    assertTrue(diff2 < 0)
}
