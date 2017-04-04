import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = arrayListOf<String?>("foo", "bar")
    val notNull = data.requireNoNulls()
    assertEquals(listOf("foo", "bar"), notNull)

    val hasNulls = listOf("foo", null, "bar")

    assertFailsWith<IllegalArgumentException> {
        // should throw an exception as we have a null
        hasNulls.requireNoNulls()
    }
}
