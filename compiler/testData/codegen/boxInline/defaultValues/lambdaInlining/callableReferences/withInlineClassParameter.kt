// SKIP_INLINE_CHECK_IN: inlineFun-1BDWgbU$default
// WITH_STDLIB
// FILE: 1.kt
package test

inline class C(val x: String)

fun foo(c: C) = c.x

inline fun inlineFun(c: C, x: (C) -> String = ::foo) = x(c)

// FILE: 2.kt
import test.*

fun box() = inlineFun(C("OK"))
