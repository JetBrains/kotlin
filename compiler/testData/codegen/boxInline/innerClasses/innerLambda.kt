// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

inline fun foo1() = run {
    {
        "OK"
    }
}

var sideEffects = "fail"

inline fun foo2() = run {
    {
        Runnable {
            sideEffects = "OK"
        }
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    val x1 = foo1()()
    if (x1 != "OK") return "fail 1: $x1"

    foo2()().run()

    return sideEffects
}
