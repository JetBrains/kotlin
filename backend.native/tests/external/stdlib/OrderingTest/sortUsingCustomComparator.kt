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
    val comparator = object : Comparator<Item> {
        override fun compare(o1: Item, o2: Item): Int {
            return compareValuesBy(o1, o2, { it.name }, { it.rating })
        }

        override fun equals(other: Any?): Boolean {
            return this == other
        }
    }
    val diff = comparator.compare(v1, v2)
    assertTrue(diff > 0)
    val items = arrayListOf(v1, v2).sortedWith(comparator)
    assertEquals(v2, items[0])
    assertEquals(v1, items[1])
}
