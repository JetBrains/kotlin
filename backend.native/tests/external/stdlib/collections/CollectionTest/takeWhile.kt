import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")
    assertEquals(emptyList<String>(), coll.takeWhile { false })
    assertEquals(coll, coll.takeWhile { true })
    assertEquals(listOf("foo"), coll.takeWhile { it.startsWith("f") })
    assertEquals(listOf("foo", "bar", "abc"), coll.takeWhile { it.length == 3 })
}
