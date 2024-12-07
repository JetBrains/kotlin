// WITH_STDLIB
// FILE: 1.kt
package test

inline fun <reified X : Enum<X>> myValueOf(): String {
    return enumValueOf<X>("OK").name
}

enum class Z {
    OK
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValueOf<Z>()
}
