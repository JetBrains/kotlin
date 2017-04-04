import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    val buffer = StringBuilder()
    data.joinTo(buffer, "-", "{", "}")
    assertEquals("{foo-bar}", buffer.toString())
}
