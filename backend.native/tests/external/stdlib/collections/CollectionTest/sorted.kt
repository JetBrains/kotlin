import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf(11, 3, 7)
    assertEquals(listOf(3, 7, 11), data.sorted())
    assertEquals(listOf(11, 7, 3), data.sortedDescending())
}
