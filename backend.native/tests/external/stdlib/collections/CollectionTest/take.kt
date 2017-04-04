import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")
    assertEquals(emptyList<String>(), coll.take(0))
    assertEquals(listOf("foo"), coll.take(1))
    assertEquals(listOf("foo", "bar"), coll.take(2))
    assertEquals(coll, coll.take(coll.size))
    assertEquals(coll, coll.take(coll.size + 1))

    assertFails { coll.take(-1) }
}
