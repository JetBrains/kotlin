// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: 1.kt
package test

open class C(val x: () -> String)

inline fun f(crossinline g: () -> String) = object : C({ g() }) {}

// FILE: 2.kt
import test.*

fun box(): String = f { "OK" }.x()
