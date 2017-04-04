import kotlin.test.*


fun box() {
    val map = linkedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
    assertEquals(1, map["a"])
    assertEquals(2, map["b"])
    assertEquals(3, map["c"])
    assertEquals(listOf("c", "b", "a"), map.keys.toList())
}
