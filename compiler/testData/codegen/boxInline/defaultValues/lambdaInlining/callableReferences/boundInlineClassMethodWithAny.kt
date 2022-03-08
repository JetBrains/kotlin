// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: 1.kt
package test

inline class C(val x: Any?) {
    fun f() = x.toString()
}

inline fun inlineFun(lambda: () -> String = C("OK")::f): String = lambda()

// FILE: 2.kt
import test.*

fun box(): String = inlineFun()
