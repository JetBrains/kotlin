// FILE: 1.kt
package test

open class C(val x: () -> String)

inline fun f(crossinline g: () -> String) = object : C({ g() }) {}

// FILE: 2.kt
import test.*

fun box(): String = f { "OK" }.x()
