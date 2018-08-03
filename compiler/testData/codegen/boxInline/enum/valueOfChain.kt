// IGNORE_BACKEND: JS_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun <reified T : Enum<T>> myValueOf(): String {
    return myValueOf2<T>()
}

inline fun <reified Y : Enum<Y>> myValueOf2(): String {
    return enumValueOf<Y>("OK").name
}


enum class Z {
    OK
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValueOf<Z>()
}
