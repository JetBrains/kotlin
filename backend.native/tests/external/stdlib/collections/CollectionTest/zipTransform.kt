import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(listOf("ab", "bc", "cd")) {
        listOf("a", "b", "c").zip(listOf("b", "c", "d")) { a, b -> a + b }
    }
}
