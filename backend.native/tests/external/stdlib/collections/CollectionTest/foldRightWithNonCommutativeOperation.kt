import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(-5) {
        val numbers = listOf(1, 2, 3)
        numbers.foldRight(7) { a, b -> a - b }
    }
}
