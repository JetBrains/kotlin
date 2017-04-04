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
    val comparator = compareBy<Item> { it.name }
    val reversed = comparator.reversed()
    assertEquals(comparator.compare(v2, v1), reversed.compare(v1, v2))
    assertEquals(comparator, reversed.reversed())
}
