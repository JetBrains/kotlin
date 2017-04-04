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

private fun propagateOf3(f3d: (Double, Double, Double) -> Double,
                         f3f: (Float, Float, Float) -> Float, function: String) {

    with(Double) {
        for (d in values) {
            assertTrue(f3d(NaN, d, POSITIVE_INFINITY).isNaN(), "$function(NaN, $d, +inf)")
            assertTrue(f3d(d, NaN, POSITIVE_INFINITY).isNaN(), "$function($d, NaN, +inf)")
            assertTrue(f3d(d, POSITIVE_INFINITY, NaN).isNaN(), "$function($d, +inf, NaN)")
        }
    }
    with(Float) {
        for (f in values) {
            assertTrue(f3f(NaN, f, POSITIVE_INFINITY).isNaN(), "$function(NaN, $f, +inf)")
            assertTrue(f3f(f, NaN, POSITIVE_INFINITY).isNaN(), "$function($f, NaN, +inf)")
            assertTrue(f3f(f, POSITIVE_INFINITY, NaN).isNaN(), "$function($f, +inf, NaN)")
        }
    }
}

fun box() {
    propagateOf2(::maxOf, ::maxOf, "maxOf")
    propagateOf3(::maxOf, ::maxOf, "maxOf")
}
