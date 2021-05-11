// KT-46601
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_MULTI_MODULE_OLD_AGAINST_IR
// SKIP_INLINE_CHECK_IN: inlineFun-1BDWgbU$default
// FILE: 1.kt
package test

inline class C(val x: String)

fun foo(c: C) = c.x

inline fun inlineFun(c: C, x: (C) -> String = ::foo) = x(c)

// FILE: 2.kt
import test.*

fun box() = inlineFun(C("OK"))
