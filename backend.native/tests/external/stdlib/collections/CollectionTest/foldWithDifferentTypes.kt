import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(7) {
        val numbers = listOf("a", "ab", "abc")
        numbers.fold(1) { a, b -> a + b.length }
    }

    expect("1234") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.fold("") { a, b -> a + b }
    }
}
