import kotlin.test.*


fun box() {
    val pairs = arrayOf("a" to 1, "b" to 2)
    val expected = mapOf(*pairs)

    val linkedMap: LinkedHashMap<String, Int> = pairs.toMap(linkedMapOf())
    assertEquals(expected, linkedMap)

    val hashMap: HashMap<String, Int> = pairs.asIterable().toMap(hashMapOf())
    assertEquals(expected, hashMap)

    val mutableMap: MutableMap<String, Int> = pairs.asSequence().toMap(mutableMapOf())
    assertEquals(expected, mutableMap)

    val mutableMap2 = mutableMap.toMap(mutableMapOf())
    assertEquals(expected, mutableMap2)

    val mutableMap3 = mutableMap.toMap(hashMapOf<CharSequence, Any>())
    assertEquals<Map<*, *>>(expected, mutableMap3)
}
