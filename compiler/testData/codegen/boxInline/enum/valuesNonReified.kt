// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt
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
