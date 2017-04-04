import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val l1: List<Int> = listOfNotNull(null)
    assertTrue(l1.isEmpty())

    val s: String? = "value"
    val l2: List<String> = listOfNotNull(s)
    assertEquals(s, l2.single())

    val l3: List<String> = listOfNotNull("value1", null, "value2")
    assertEquals(listOf("value1", "value2"), l3)
}
