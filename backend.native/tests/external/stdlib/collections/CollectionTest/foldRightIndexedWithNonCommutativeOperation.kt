import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(-4) {
        val numbers = listOf(1, 2, 3)
        numbers.foldRightIndexed(7) { index, a, b -> index + a - b }
    }
}
