// IGNORE_BACKEND: NATIVE
import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.comparisons.*
import kotlin.test.assertEquals

private fun <T : Comparable<T>> totalOrderMinOf2(f2t: (T, T) -> T, function: String) {
    @Suppress("UNCHECKED_CAST")
    with(Double) {
        assertEquals<Any>(0.0, f2t(0.0 as T, NaN as T), "$function(0, NaN)")
        assertEquals<Any>(0.0, f2t(NaN as T, 0.0 as T), "$function(NaN, 0)")
    }
}

fun box() {
    totalOrderMinOf2<Comparable<Any>>(::minOf, "minOf")
}
