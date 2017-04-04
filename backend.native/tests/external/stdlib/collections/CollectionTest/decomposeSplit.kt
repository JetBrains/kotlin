import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val (key, value) = "key = value".split("=").map { it.trim() }
    assertEquals(key, "key")
    assertEquals(value, "value")
}
