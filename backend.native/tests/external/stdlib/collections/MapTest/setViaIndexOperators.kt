import kotlin.test.*


fun box() {
    val map = hashMapOf<String, String>()
    assertTrue { map.none() }
    assertEquals(map.size, 0)

    map["name"] = "James"

    assertTrue { map.any() }
    assertEquals(map.size, 1)
    assertEquals("James", map["name"])
}
