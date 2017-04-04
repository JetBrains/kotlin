import kotlin.test.*


fun box() {
    val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
    val list = arrayListOf<String>()
    for (e in map) {
        list.add(e.key)
        list.add(e.value)
    }

    assertEquals(6, list.size)
    assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
}
