// FILE: 1.kt
package test

inline fun <reified T> f(x : () -> T): Array<T> = Array(1) { x() }

// FILE: 2.kt

import test.*

fun box(): String = f { "OK" }[0]
