import kotlin.test.*


fun box() {
    val data = mapOf<String, Int>()
    val a = data.getOrElse("foo") { 2 }
    assertEquals(2, a)
    val a1 = data.getOrElse("foo") { data.get("bar") } ?: 1
    assertEquals(1, a1)

    val b = data.getOrElse("foo") { 3 }
    assertEquals(3, b)
    assertEquals(0, data.size)

    val empty = mapOf<String, Int?>()
    val c = empty.getOrElse("") { null }
    assertEquals(null, c)

    val nullable = mapOf(1 to null)
    val d = nullable.getOrElse(1) { "x" }
    assertEquals("x", d)
}
