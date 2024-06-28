// NO_CHECK_LAMBDA_INLINING
// IGNORE_INLINER_K2: IR
// FILE: 1.kt

package zzz


inline fun calc(crossinline lambda: () -> Int): Int {
    return doCalc { lambda() }
}

fun doCalc(lambda2: () -> Int): Int {
    return lambda2()
}

// FILE: 2.kt

import zzz.*

fun box(): String {

    val p = { calc { 11 } }.let { it() }

    val z = { calc { 12 } }.let { it() }

    if (p == z) return "fail"

    return "OK"
}
