import kotlin.test.*


fun <T> assertStaticTypeIs(value: T) {}
fun box() {
    val m1 = mapOf("beverage" to "beer", "location" to "Mells")
    val m2 = m1.mapKeys { it.key + "2" }

    assertEquals(mapOf("beverage2" to "beer", "location2" to "Mells"), m2)

    val m1p: Map<out String, String> = m1
    val m3 = m1p.mapKeysTo(mutableMapOf()) { it.key.length }
    assertStaticTypeIs<MutableMap<Int, String>>(m3)
    assertEquals(mapOf(8 to "Mells"), m3)
}
