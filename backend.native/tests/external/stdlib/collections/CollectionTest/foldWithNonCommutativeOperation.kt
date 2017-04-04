import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(1) {
        val numbers = listOf(1, 2, 3)
        numbers.fold(7) { a, b -> a - b }
    }
}
