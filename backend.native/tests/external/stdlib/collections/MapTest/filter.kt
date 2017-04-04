import kotlin.test.*


fun box() {
    val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
    val filteredByKey = map.filter { it.key[0] == 'b' }
    assertEquals(mapOf("b" to 3), filteredByKey)

    val filteredByKey2 = map.filterKeys { it[0] == 'b' }
    assertEquals(mapOf("b" to 3), filteredByKey2)

    val filteredByValue = map.filter { it.value == 2 }
    assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue)

    val filteredByValue2 = map.filterValues { it % 2 == 0 }
    assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue2)
}
