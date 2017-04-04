import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    assertEquals("foo", data.first())
    assertEquals(15, listOf(15, 19, 20, 25).first())
    assertEquals('a', listOf('a').first())
    assertFails { arrayListOf<Int>().first() }
}
