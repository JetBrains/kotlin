// IGNORE_INLINER: IR
// TARGET_BACKEND: JVM
// FILE: utils.kt
package a

public inline fun <reified T> example(): T? {
    nop {
        "" is T
    }
    return null
}

fun nop(x: Runnable) = x.run()

// FILE: main.kt

import a.example

fun box(): String {
    example<String>()
    return "OK"
}
