// FILE: 1.kt
package test

inline fun build(noinline pathFunc: (() -> String)? = null) {
    pathFunc?.invoke()
}

// FILE: 2.kt

import test.*

fun box(): String {
    build ()
    return "OK"
}