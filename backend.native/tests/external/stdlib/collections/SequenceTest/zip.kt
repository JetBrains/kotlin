import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    expect(listOf("ab", "bc", "cd")) {
        sequenceOf("a", "b", "c").zip(sequenceOf("b", "c", "d")) { a, b -> a + b }.toList()
    }
}
