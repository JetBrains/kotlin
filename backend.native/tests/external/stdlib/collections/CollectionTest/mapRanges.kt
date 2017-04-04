import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val range = (1..3).map { it * 2 }
    assertEquals(listOf(2, 4, 6), range)
}
