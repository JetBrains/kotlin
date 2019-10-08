// FILE: 1.kt
package test

inline fun <reified T> f(): Array<T> = arrayOf()

// FILE: 2.kt

import test.*

fun box(): String {
    f<String>()
    return "OK"
}
