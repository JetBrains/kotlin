import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val src: Array<Any> = arrayOf(1, 2, 3.0, "abc", "cde")

    val numberValues: List<Number> = src.filterIsInstance<Number>()
    assertEquals(listOf(1, 2, 3.0), numberValues)

    // doesn't distinguish double from int in JS
//        val doubleValues: List<Double> = src.filterIsInstance<Double>()
//        assertEquals(listOf(3.0), doubleValues)

    val stringValues: List<String> = src.filterIsInstance<String>()
    assertEquals(listOf("abc", "cde"), stringValues)

    // is Any doesn't work in JS, see KT-7665
//        val anyValues: List<Any> = src.filterIsInstance<Any>()
//        assertEquals(src.toList(), anyValues)

    val charValues: List<Char> = src.filterIsInstance<Char>()
    assertEquals(0, charValues.size)
}
