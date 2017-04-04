import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf(11, 3, 7)

    val asc = data.toMutableList()
    asc.sort()
    assertEquals(listOf(3, 7, 11), asc)

    val desc = data.toMutableList()
    desc.sortDescending()
    assertEquals(listOf(11, 7, 3), desc)
}
