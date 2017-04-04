import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(42) {
        val numbers = listOf(1, 2, 3, 4)
        numbers.foldIndexed(0) { index, a, b -> index * (a + b) }
    }

    expect(0) {
        val numbers = arrayListOf<Int>()
        numbers.foldIndexed(0) { index, a, b -> index * (a + b) }
    }

    expect("11234") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.map { it.toString() }.foldIndexed("") { index, a, b -> if (index == 0) a + b + b else a + b }
    }
}
