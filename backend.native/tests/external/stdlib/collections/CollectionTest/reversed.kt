import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    val rev = data.reversed()
    assertEquals(listOf("bar", "foo"), rev)
    assertNotEquals(data, rev)
}
