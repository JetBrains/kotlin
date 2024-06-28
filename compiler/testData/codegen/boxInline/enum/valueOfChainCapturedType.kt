// WITH_STDLIB
// FILE: 1.kt
package test

inline fun <reified Z : Enum<Z>> myValueOf(): String {
    return myValueOf2<Z>()
}

inline fun <reified Y : Enum<Y>> myValueOf2(): String {
    return { enumValueOf<Y>("OK").name }.let { it() }
}


enum class Z {
    OK
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValueOf<Z>()
}
