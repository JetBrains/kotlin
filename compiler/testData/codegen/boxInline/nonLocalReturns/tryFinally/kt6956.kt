// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun baz(x: Int) {}

inline fun <T> foo(action: () -> T): T {
    baz(0)
    try {
        return action()
    } finally {
        baz(1)
    }
}

// FILE: 2.kt

import test.*

inline fun <T> bar(arg: String, action: () -> T) {
    try {
        action()
    } finally {
        arg.length
    }
}

fun box(): String {
    foo() {
        bar("") {
            return "OK"
        }
    }

    return "fail"
}
