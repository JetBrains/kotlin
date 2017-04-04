import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    var c = 0
    expect(5, { listOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
    assertEquals(5, c)
    c = 0
    expect(5, { listOf(5, 4, 3, 2, 1).asSequence().maxBy { c++; it * it } })
    assertEquals(5, c)
}
