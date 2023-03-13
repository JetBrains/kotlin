// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt
package test

inline fun <reified T : Enum<T>> myValues(): Array<T> {
    return enumValues<T>()
}

enum class Z {
    O, K;

    val myParam = name
}


// FILE: 2.kt

import test.*

fun box(): String {
    return test(myValues<Z>())
}

fun test(myValues: Array<Z>): String {
    return myValues.map { it.myParam }.joinToString ("");
}
