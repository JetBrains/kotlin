import kotlin.test.*

import kotlin.comparisons.*

data class Item(val name: String, val rating: Int) : Comparable<Item> {
    public override fun compareTo(other: Item): Int {
        return compareValuesBy(this, other, { it.rating }, { it.name })
    }
}

val v1 = Item("wine", 9)
val v2 = Item("beer", 10)
val v3 = Item("apple", 20)
fun box() {
    val byName = compareBy<Item> { it.name }
    val byRating = compareBy<Item> { it.rating }
    val v3 = Item(v1.name, v1.rating + 1)
    val v4 = Item(v2.name + "_", v2.rating)
    assertTrue((byName then byRating).compare(v1, v2) > 0)
    assertTrue((byName then byRating).compare(v1, v3) < 0)
    assertTrue((byName thenDescending byRating).compare(v1, v3) > 0)

    assertTrue((byRating then byName).compare(v1, v2) < 0)
    assertTrue((byRating then byName).compare(v4, v2) > 0)
    assertTrue((byRating thenDescending byName).compare(v4, v2) < 0)
}
