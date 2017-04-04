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
    assertEquals(v2, minOf(v1, v2, compareBy { it.name }))
    assertEquals(v3, minOf(v3, v2, v1, compareBy { it.name }))
    assertEquals(v1, minOf(v1, v2, compareBy { it.rating }))
    assertEquals(v1, minOf(v1, v2, v3, compareBy { it.rating }))
}
