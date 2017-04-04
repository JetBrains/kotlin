import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect("1234") {
        val list = listOf("1", "2", "3", "4")
        list.reduce { a, b -> a + b }
    }

    assertFailsWith<UnsupportedOperationException> {
        arrayListOf<Int>().reduce { a, b -> a + b }
    }
}
