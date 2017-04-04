import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect("12343210") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.foldRightIndexed("") { index, a, b -> "" + a + b + index }
    }
}
