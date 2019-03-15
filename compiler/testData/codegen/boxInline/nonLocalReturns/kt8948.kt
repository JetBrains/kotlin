// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun foo(f: () -> Unit) {
    try {
        f()
    }
    finally {
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    foo {
        try {
            return "OK"
        } catch(e: Exception) {
            return "fail 1"
        }
    }

    return "fail 2"
}
