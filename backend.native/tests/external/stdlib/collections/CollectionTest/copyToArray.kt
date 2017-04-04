import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    val arr = data.toTypedArray()
    println("Got array ${arr}")
    assertEquals(2, arr.size)
}
