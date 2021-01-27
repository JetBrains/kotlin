// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun String.fire(message: String? = null) {
    val res = this + message!!
}

// FILE: 2.kt

import test.*

fun box(): String {
    val receiver = "receiver"
    "".let {
        {
            receiver.fire()
        }
    }
    return "OK"
}
