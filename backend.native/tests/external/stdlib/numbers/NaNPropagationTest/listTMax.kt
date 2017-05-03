// IGNORE_BACKEND: NATIVE
import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.comparisons.*
import kotlin.test.assertEquals

private fun <T : Comparable<T>> totalOrderMaxOf2(f2t: (T, T) -> T, function: String) {
    @Suppress("UNCHECKED_CAST")
    with(Double) {
        assertTrue((f2t(0.0 as T, NaN as T) as Double).isNaN(), "$function(0, NaN)")
        assertTrue((f2t(NaN as T, 0.0 as T) as Double).isNaN(), "$function(NaN, 0)")
    }
}

fun box() {
    totalOrderMaxOf2<Comparable<Any>>({ a, b -> listOf(a, b).max()!! }, "listOf().max()")
}
