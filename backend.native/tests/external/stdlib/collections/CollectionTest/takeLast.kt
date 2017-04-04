import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")

    assertEquals(emptyList<String>(), coll.takeLast(0))
    assertEquals(listOf("abc"), coll.takeLast(1))
    assertEquals(listOf("bar", "abc"), coll.takeLast(2))
    assertEquals(coll, coll.takeLast(coll.size))
    assertEquals(coll, coll.takeLast(coll.size + 1))

    assertFails { coll.takeLast(-1) }
}
