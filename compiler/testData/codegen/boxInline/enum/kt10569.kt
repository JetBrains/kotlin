// WITH_STDLIB
// FILE: 1.kt
package test

var result = ""

inline fun <reified T : Enum<T>> renderOptions(render: (T) -> String) {
    val values = enumValues<T>()
    for (v in values) {
        result += render(v)
    }
}

enum class Z {
    O, K;

    val myParam = name
}


// FILE: 2.kt

import test.*

fun box(): String {
    renderOptions<Z> {
        it.myParam
    }
    return result
}
