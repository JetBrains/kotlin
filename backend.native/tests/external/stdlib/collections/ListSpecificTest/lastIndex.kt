import kotlin.test.*

val data = listOf("foo", "bar")
val empty = listOf<String>()
fun box() {
    assertEquals(-1, empty.lastIndex)
    assertEquals(1, data.lastIndex)
}
