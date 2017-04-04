import kotlin.test.*


fun <T> assertStaticTypeIs(value: T) {}
fun box() {
    val m1 = mapOf("beverage" to "beer", "location" to "Mells")
    val m2 = m1.mapValues { it.value + "2" }

    assertEquals(mapOf("beverage" to "beer2", "location" to "Mells2"), m2)

    val m1p: Map<out String, String> = m1
    val m3 = m1p.mapValuesTo(hashMapOf()) { it.value.length }
    assertStaticTypeIs<HashMap<String, Int>>(m3)
    assertEquals(mapOf("beverage" to 4, "location" to 5), m3)
}
