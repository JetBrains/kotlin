// FILE: 1.kt
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
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
