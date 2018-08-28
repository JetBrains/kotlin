// IGNORE_BACKEND: JS_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun myValueOf(): String {
    return enumValueOf<Z>("OK").name
}

enum class Z {
    OK
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValueOf()
}
