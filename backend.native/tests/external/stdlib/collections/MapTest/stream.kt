import kotlin.test.*


fun box() {
    val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
    val named = map.asSequence().filter { it.key == "name" }.single()
    assertEquals("James", named.value)
}
