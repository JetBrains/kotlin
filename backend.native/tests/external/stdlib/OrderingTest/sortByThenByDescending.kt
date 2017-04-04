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
    val comparator = compareBy<Item> { it.rating }.thenByDescending { it.name }

    val diff = comparator.compare(v1, v2)
    assertTrue(diff < 0)
    val items = arrayListOf(v1, v2).sortedWith(comparator)
    assertEquals(v1, items[0])
    assertEquals(v2, items[1])
}
