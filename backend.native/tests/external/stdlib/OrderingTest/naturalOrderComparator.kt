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
    val v1 = "a"
    val v2 = "beta"

    assertTrue(naturalOrder<String>().compare(v1, v2) < 0)
    assertTrue(reverseOrder<String>().compare(v1, v2) > 0)
    assertTrue(reverseOrder<Int>() === naturalOrder<Int>().reversed())
    assertTrue(naturalOrder<Int>() === reverseOrder<Int>().reversed())
}
