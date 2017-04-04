import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(listOf("a" to "b", "b" to "c", "c" to "d")) {
        listOf("a", "b", "c").zip(listOf("b", "c", "d"))
    }
}
