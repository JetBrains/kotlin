import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    // lets calculate the sum of some numbers
    expect(10) {
        val numbers = listOf(1, 2, 3, 4)
        numbers.fold(0) { a, b -> a + b }
    }

    expect(0) {
        val numbers = arrayListOf<Int>()
        numbers.fold(0) { a, b -> a + b }
    }

    // lets concatenate some strings
    expect("1234") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.map { it.toString() }.fold("") { a, b -> a + b }
    }
}
