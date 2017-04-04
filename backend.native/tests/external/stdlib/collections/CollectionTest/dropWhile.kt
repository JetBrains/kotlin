import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")
    assertEquals(listOf("bar", "abc"), coll.dropWhile { it.startsWith("f") })
}
