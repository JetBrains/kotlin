import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect("12343210") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.map { it.toString() }.foldRightIndexed("") { index, a, b -> a + b + index }
    }
}
