// FILE: 1.kt

package test

inline fun foo(f: () -> Unit) {
    try {
        f()
    }
    finally {
        1
    }
}

// FILE: 2.kt

// TODO: enabled when KT-6397 gets fixed
// TARGET_BACKEND: JVM
import test.*

var p = "fail"

fun test() {
    foo {
        try {
            p = "O"
            return
        } catch(e: Exception) {
            return
        } finally {
            p += "K"
        }
    }
}

fun box(): String {
    test()
    return p
}
