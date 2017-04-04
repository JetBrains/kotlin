import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("foo", "bar", "abc")
    assertEquals(emptyList<String>(), coll.takeLastWhile { false })
    assertEquals(coll, coll.takeLastWhile { true })
    assertEquals(listOf("abc"), coll.takeLastWhile { it.startsWith("a") })
    assertEquals(listOf("bar", "abc"), coll.takeLastWhile { it[0] < 'c' })
}
