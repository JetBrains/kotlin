import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = arrayListOf<String>()
    data.reverse()
    assertTrue(data.isEmpty())

    data.add("foo")
    data.reverse()
    assertEquals(listOf("foo"), data)

    data.add("bar")
    data.reverse()
    assertEquals(listOf("bar", "foo"), data)

    data.add("zoo")
    data.reverse()
    assertEquals(listOf("zoo", "foo", "bar"), data)
}
