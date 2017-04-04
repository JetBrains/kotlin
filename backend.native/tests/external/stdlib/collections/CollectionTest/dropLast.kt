import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")
    assertEquals(coll, coll.dropLast(0))
    assertEquals(emptyList<String>(), coll.dropLast(coll.size))
    assertEquals(emptyList<String>(), coll.dropLast(coll.size + 1))
    assertEquals(listOf("foo", "bar"), coll.dropLast(1))
    assertEquals(listOf("foo"), coll.dropLast(2))

    assertFails { coll.dropLast(-1) }
}
