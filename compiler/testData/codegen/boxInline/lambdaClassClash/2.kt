package zzz

inline fun calc(lambda: () -> Int): Int {
    return doCalc { lambda() }
}

fun doCalc(lambda2: () -> Int): Int {
    return lambda2()
}
