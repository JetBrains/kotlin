// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// FILE: 1.kt
package test

inline class C(val value: Any?)

fun foo(x: Any?): C = x as C

inline fun inlineFun(s: (C) -> Any? = ::foo): Any? = s(C("OK"))

// FILE: 2.kt
import test.*

fun box(): String = (inlineFun() as C).value as String
