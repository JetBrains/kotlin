import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    assertEquals("bar", data.last())
    assertEquals(25, listOf(15, 19, 20, 25).last())
    assertEquals('a', listOf('a').last())
    assertFails { arrayListOf<Int>().last() }
}
