// IGNORE_BACKEND: JS_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun myValues(): String {
    val values = enumValues<Z>()
    return values.joinToString("")
}

enum class Z {
    O, K
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValues()
}
