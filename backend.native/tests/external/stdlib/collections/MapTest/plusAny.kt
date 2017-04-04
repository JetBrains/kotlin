import kotlin.test.*

fun <K, V> testPlusAny(mapObject: Any, pair: Pair<K, V>) {
    val map = mapObject as Map<*, *>
    fun assertContains(map: Map<*, *>) = assertEquals(pair.second, map[pair.first])

    assertContains(map + pair)
    assertContains(map + listOf(pair))
    assertContains(map + arrayOf(pair))
    assertContains(map + sequenceOf(pair))
    assertContains(map + mapOf(pair))
}

fun box() {
    testPlusAny(emptyMap<String, String>(), 1 to "A")
    testPlusAny(mapOf("A" to null), "A" as CharSequence to 2)
}
