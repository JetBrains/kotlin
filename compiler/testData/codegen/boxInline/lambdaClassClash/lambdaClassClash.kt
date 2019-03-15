// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package zzz


inline fun calc(crossinline lambda: () -> Int): Int {
    return doCalc { lambda() }
}

fun doCalc(lambda2: () -> Int): Int {
    return lambda2()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import zzz.*

fun box(): String {

    val p = { calc { 11 }} ()

    val z = { calc { 12 }}()

    if (p == z) return "fail"

    return "OK"
}
