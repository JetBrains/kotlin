import kotlin.test.*


fun box() {
    val m1 = mapOf("beverage" to "beer", "location" to "Mells")
    val list = m1.map { it.value + " rocks" }

    assertEquals(listOf("beer rocks", "Mells rocks"), list)
}
