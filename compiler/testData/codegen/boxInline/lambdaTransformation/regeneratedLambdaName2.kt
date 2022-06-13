// NO_CHECK_LAMBDA_INLINING
// CHECK_BYTECODE_LISTING
// FILE: 1.kt
package test

inline fun <R> call(crossinline f: () -> R) : R {
    return { f() }.let { it() }
}

// FILE: 2.kt
import test.*

inline fun sameName(s: Long): String = call { "FAIL" }
inline fun sameName(s: Int): String = call { "OK" }

fun box(): String {
    val result = sameName(1)
    sameName(1L)
    return result
}
