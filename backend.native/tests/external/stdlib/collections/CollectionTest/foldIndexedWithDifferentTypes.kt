import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(10) {
        val numbers = listOf("a", "ab", "abc")
        numbers.foldIndexed(1) { index, a, b -> a + b.length + index }
    }

    expect("11223344") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.foldIndexed("") { index, a, b -> a + b + (index + 1) }
    }
}
