import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val values: List<Any> = listOf(1, 2, 3.0, "abc", "cde")

    val numberValues: List<Number> = values.filterIsInstance<Number>()
    assertEquals(listOf(1, 2, 3.0), numberValues)

    // doesn't distinguish double from int in JS
//        val doubleValues: List<Double> = values.filterIsInstance<Double>()
//        assertEquals(listOf(3.0), doubleValues)

    val stringValues: List<String> = values.filterIsInstance<String>()
    assertEquals(listOf("abc", "cde"), stringValues)

    // is Any doesn't work in JS, see KT-7665
//        val anyValues: List<Any> = values.filterIsInstance<Any>()
//        assertEquals(values.toList(), anyValues)

    val charValues: List<Char> = values.filterIsInstance<Char>()
    assertEquals(0, charValues.size)
}
