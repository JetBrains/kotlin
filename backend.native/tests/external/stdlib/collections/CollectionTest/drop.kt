import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")
    assertEquals(listOf("bar", "abc"), coll.drop(1))
    assertEquals(listOf("abc"), coll.drop(2))
}
