import kotlin.test.*


fun box() {
    val data = hashMapOf<String, Int>()
    val a = data.getOrPut("foo") { 2 }
    assertEquals(2, a)

    val b = data.getOrPut("foo") { 3 }
    assertEquals(2, b)

    assertEquals(1, data.size)

    val empty = hashMapOf<String, Int?>()
    val c = empty.getOrPut("") { null }
    assertEquals(null, c)

    val d = empty.getOrPut("") { 1 }
    assertEquals(1, d)
}
