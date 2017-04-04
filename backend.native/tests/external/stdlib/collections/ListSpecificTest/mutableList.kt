import kotlin.test.*

fun box() {
    val items = listOf("beverage", "location", "name")

    var list = listOf<String>()
    for (item in items) {
        list += item
    }

    assertEquals(3, list.size)
    assertEquals("beverage,location,name", list.joinToString(","))
}
