import kotlin.test.*


fun box() {
    val elements = listOf("foo", "bar", "flea", "zoo", "biscuit")
    val counts = elements.groupingBy { it.first() }.eachCount()

    assertEquals(mapOf('f' to 2, 'b' to 2, 'z' to 1), counts)

    val elements2 = arrayOf("zebra", "baz", "cab")
    val counts2 = elements2.groupingBy { it.last() }.eachCountTo(HashMap(counts))

    assertEquals(mapOf('f' to 2, 'b' to 3, 'a' to 1, 'z' to 2), counts2)
}
