// IGNORE_BACKEND: JS_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun stub() {}

enum class Z {
    OK
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return  { enumValueOf<Z>("OK").name  } ()
}
