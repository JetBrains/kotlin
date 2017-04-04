import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val list = listOf("foo", "bar") + listOf("cheese", "wine")
    assertEquals(listOf("foo", "bar", "cheese", "wine"), list)
}
