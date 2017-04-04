import kotlin.test.*


fun <T> assertStaticTypeIs(value: T) {}
fun box() {
    val map: Map<out String, Int> = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))

    val filteredByKey = map.filterTo(mutableMapOf()) { it.key[0] == 'b' }
    assertStaticTypeIs<MutableMap<String, Int>>(filteredByKey)
    assertEquals(mapOf("b" to 3), filteredByKey)

    val filteredByKey2 = map.filterKeys { it[0] == 'b' }
    assertStaticTypeIs<Map<String, Int>>(filteredByKey2)
    assertEquals(mapOf("b" to 3), filteredByKey2)

    val filteredByValue = map.filterNotTo(hashMapOf()) { it.value != 2 }
    assertStaticTypeIs<HashMap<String, Int>>(filteredByValue)
    assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue)

    val filteredByValue2 = map.filterValues { it % 2 == 0 }
    assertStaticTypeIs<Map<String, Int>>(filteredByValue2)
    assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue2)
}
