// FILE: 1.kt

package test


inline fun <R> call(crossinline f: () -> R) : R {
    return {f()} ()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun sameName(s: Long): Long {
    return call {
        s
    }
}

fun sameName(s: Int): Int {
    return call {
        s
    }
}

fun box(): String {
    val result = sameName(1.toLong())
    if (result != 1.toLong()) return "fail1: ${result}"

    val result2 = sameName(2)
    if (result2 != 2) return "fail2: ${result2}"

    return "OK"
}
