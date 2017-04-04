import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar", "something", "xyz")
    val pair = data.partition { it.length == 3 }

    assertEquals(listOf("foo", "bar", "xyz"), pair.first, "pair.first")
    assertEquals(listOf("something"), pair.second, "pair.second")
}
