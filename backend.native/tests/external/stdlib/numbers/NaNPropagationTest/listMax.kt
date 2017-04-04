import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.comparisons.*
import kotlin.test.assertEquals

val Double.Companion.values get() = listOf(0.0, NEGATIVE_INFINITY, MIN_VALUE, MAX_VALUE, POSITIVE_INFINITY, NaN)
val Float.Companion.values get() = listOf(0.0f, NEGATIVE_INFINITY, MIN_VALUE, MAX_VALUE, POSITIVE_INFINITY, NaN)
private fun propagateOf2(f2d: (Double, Double) -> Double,
                         f2f: (Float, Float) -> Float,
                         function: String) {
    with(Double) {
        for (d in values) {
            assertTrue(f2d(d, NaN).isNaN(), "$function($d, NaN)")
            assertTrue(f2d(NaN, d).isNaN(), "$function(NaN, $d)")
        }
    }
    with(Float) {
        for (f in values) {
            assertTrue(f2f(f, NaN).isNaN(), "$function($f, NaN)")
            assertTrue(f2f(NaN, f).isNaN(), "$function(NaN, $f)")
        }
    }
}

fun box() {
    propagateOf2({ a, b -> listOf(a, b).max()!! },
            { a, b -> listOf(a, b).max()!! },
            "listOf().max()")
}
