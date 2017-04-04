import kotlin.test.*


fun box() {
    val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
    val filteredByKey = map.filterNot { it.key == "b" }
    assertEquals(2, filteredByKey.size)
    assertEquals(null, filteredByKey["b"])
    assertEquals(2, filteredByKey["c"])
    assertEquals(2, filteredByKey["a"])

    val filteredByValue = map.filterNot { it.value == 2 }
    assertEquals(1, filteredByValue.size)
    assertEquals(3, filteredByValue["b"])
}
