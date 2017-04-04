import kotlin.test.*


fun box() {
    val map = mutableMapOf("beverage" to "beer", "location" to "Mells")
    val result = StringBuilder()
    val newMap = map.onEach { result.append(it.key).append("=").append(it.value).append(";") }
    assertEquals("beverage=beer;location=Mells;", result.toString())
    assertTrue(map === newMap)

    // static types test
    val m: HashMap<String, String> = hashMapOf("a" to "b").onEach { }
}
