import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect("1234") {
        val numbers = listOf(1, 2, 3, 4)
        numbers.foldRight("") { a, b -> "" + a + b }
    }
}
