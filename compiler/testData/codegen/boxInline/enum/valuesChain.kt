// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt
package test

inline fun <reified Y : Enum<Y>> myValues2(): String {
    val values = enumValues<Y>()
    return values.joinToString("")
}

inline fun <reified T : Enum<T>> myValues(): String {
    return myValues2<T>()
}

enum class Z {
    O, K
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValues<Z>()
}
