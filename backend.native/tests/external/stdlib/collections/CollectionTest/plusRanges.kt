import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val range1 = 1..3
    val range2 = 4..7
    val combined = range1 + range2
    assertEquals((1..7).toList(), combined)
}
