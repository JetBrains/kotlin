// FILE: 1.kt

package test

fun call() = inlineFun2 { stub()}

internal inline fun inlineFun2(p: () -> Unit): String {
    p()

    return inlineFun {
        test()
    }
}

private fun stub() = "fail"

private fun test() = "OK"

inline internal fun inlineFun(p: () -> String): String {
    return p()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return call()
}
