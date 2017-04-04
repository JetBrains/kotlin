import kotlin.test.*


fun box() {
    val elements = listOf("foo", "bar", "flea", "zoo", "biscuit")
    // only collect strings with even length
    val result = elements.groupingBy { it.first() }.fold(listOf<String>()) { acc, e -> if (e.length % 2 == 0) acc + e else acc }

    assertEquals(mapOf('f' to listOf("flea"), 'b' to emptyList(), 'z' to emptyList()), result)

    val moreElements = listOf("fire", "zero", "abstract")
    val result2 = moreElements.groupingBy { it.first() }.foldTo(HashMap(result), listOf()) { acc, e -> if (e.length % 2 == 0) acc + e else acc }

    assertEquals(mapOf('f' to listOf("flea", "fire"), 'b' to emptyList(), 'z' to listOf("zero"), 'a' to listOf("abstract")), result2)
}
