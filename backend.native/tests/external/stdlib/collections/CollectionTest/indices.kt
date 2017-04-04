import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    val indices = data.indices
    assertEquals(0, indices.start)
    assertEquals(1, indices.endInclusive)
    assertEquals(0..data.size - 1, indices)
}
