import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = arrayListOf("aa" to 20, "ab" to 3, "aa" to 3)
    data.sortBy { it.second }
    assertEquals(listOf("ab" to 3, "aa" to 3, "aa" to 20), data)

    data.sortBy { it.first }
    assertEquals(listOf("aa" to 3, "aa" to 20, "ab" to 3), data)

    data.sortByDescending { (it.first + it.second).length }
    assertEquals(listOf("aa" to 20, "aa" to 3, "ab" to 3), data)
}
