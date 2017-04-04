import kotlin.test.*


fun box() {
    val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
    assertEquals(3, map.count())

    val filteredByKey = map.count { it.key == "b" }
    assertEquals(1, filteredByKey)

    val filteredByValue = map.count { it.value == 2 }
    assertEquals(2, filteredByValue)
}
