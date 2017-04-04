import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    assertEquals(2, data.count())
    assertEquals(3, hashSetOf(12, 14, 15).count())
    assertEquals(0, ArrayList<Double>().count())
}
