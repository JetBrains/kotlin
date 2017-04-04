import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    assertEquals(listOf("two" to 3, "three" to 20), listOf("three" to 20, "two" to 3).sortedBy { it.second })
    assertEquals(listOf("three" to 20, "two" to 3), listOf("three" to 20, "two" to 3).sortedBy { it.first })
    assertEquals(listOf("three", "two"), listOf("two", "three").sortedByDescending { it.length })
}
