// FILE: 1.kt

package test

inline fun <reified T> f(x : () -> Unit) {
    object { init { "OK" is T } }
}

// FILE: 2.kt

import test.*

fun box(): String {
    f<String>() {}
    return "OK"
}
