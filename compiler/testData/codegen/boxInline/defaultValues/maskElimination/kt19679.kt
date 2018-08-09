// FILE: 1.kt
package test

inline fun build(func: () -> Unit, noinline pathFunc: (() -> String)? = null) {
    func()

    pathFunc?.invoke()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    build({ result = "OK" })

    return result
}