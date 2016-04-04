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

import test.*

var p = "fail"

fun test1() {
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
    test1()
    return p
}
