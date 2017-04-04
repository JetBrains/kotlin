import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val list = List(3) { index -> "x".repeat(index + 1) }
    assertEquals(3, list.size)
    assertEquals(listOf("x", "xx", "xxx"), list)
}
