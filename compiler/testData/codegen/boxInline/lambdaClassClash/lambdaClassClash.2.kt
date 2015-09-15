package zzz

import kotlin.InlineOption.*

inline fun calc(crossinline lambda: () -> Int): Int {
    return doCalc { lambda() }
}

fun doCalc(lambda2: () -> Int): Int {
    return lambda2()
}
