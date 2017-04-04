import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val source = listOf("foo", "bar")
    val list = arrayListOf<String>()
    val result = source - list

    list += "foo"
    assertEquals(source, result)
    list += "bar"
    assertEquals(source, result)
}
